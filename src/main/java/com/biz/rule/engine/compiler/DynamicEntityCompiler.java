package com.biz.rule.engine.compiler;

import com.biz.rule.common.exception.BizException;
import com.biz.rule.engine.model.EntityModel;
import lombok.extern.slf4j.Slf4j;

import javax.tools.DiagnosticCollector;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.FileObject;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 动态实体类编译器
 * 基于 JDK 原生 JavaCompiler，将 EntityModel 编译为 Class 字节码，
 * 并通过自定义 ClassLoader 加载，供 Drools 引擎引用。
 */
@Slf4j
public class DynamicEntityCompiler {

    /**
     * 根据实体模型生成 Java 源码
     */
    public String generateSource(EntityModel model) {
        StringBuilder sb = new StringBuilder(2048);
        sb.append("package ").append(model.getPackageName()).append(";\n\n");

        if (model.getImports() != null) {
            for (String imp : model.getImports()) {
                if (imp != null && !imp.isEmpty()) {
                    sb.append("import ").append(imp).append(";\n");
                }
            }
            sb.append("\n");
        }

        sb.append("public class ").append(model.getClassName()).append(" implements java.io.Serializable {\n");
        sb.append("    private static final long serialVersionUID = 1L;\n\n");

        // 字段
        if (model.getFields() != null) {
            for (EntityModel.FieldDef f : model.getFields()) {
                if (f.getComment() != null && !f.getComment().isEmpty()) {
                    sb.append("    /** ").append(f.getComment()).append(" */\n");
                }
                sb.append("    private ").append(f.getType()).append(" ").append(f.getName()).append(";\n");
            }
            sb.append("\n");

            // getter / setter
            for (EntityModel.FieldDef f : model.getFields()) {
                String cap = capitalize(f.getName());
                sb.append("    public ").append(f.getType()).append(" get").append(cap).append("() { return ").append(f.getName()).append("; }\n");
                sb.append("    public void set").append(cap).append("(").append(f.getType()).append(" ").append(f.getName()).append(") { this.").append(f.getName()).append(" = ").append(f.getName()).append("; }\n");
            }
        }

        sb.append("}\n");
        return sb.toString();
    }

    /**
     * 编译实体模型为 Class 对象
     */
    public Class<?> compile(EntityModel model) {
        String source = generateSource(model);
        String fullClassName = model.getPackageName() + "." + model.getClassName();
        log.info("开始编译动态实体: {}", fullClassName);

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new BizException("当前JDK未提供JavaCompiler，请使用JDK（非JRE）运行");
        }

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        StandardJavaFileManager standardFileManager =
                compiler.getStandardFileManager(diagnostics, null, null);

        MemoryFileManager fileManager = new MemoryFileManager(standardFileManager);

        JavaFileObject sourceObject = new JavaSourceObject(fullClassName, source);

        List<String> options = new ArrayList<>();
        options.add("-encoding");
        options.add("UTF-8");

        JavaCompiler.CompilationTask task = compiler.getTask(
                null, fileManager, diagnostics, options, null,
                Collections.singletonList(sourceObject));

        boolean ok = task.call();
        if (!ok) {
            String diag = diagnostics.getDiagnostics().toString();
            log.error("实体编译失败: {}\n{}", fullClassName, diag);
            throw new BizException("实体编译失败: " + diag);
        }

        try {
            fileManager.close();
        } catch (IOException e) {
            log.warn("关闭FileManager异常", e);
        }

        MemoryClassLoader classLoader = new MemoryClassLoader(fileManager.getClassBytes());
        try {
            return classLoader.loadClass(fullClassName);
        } catch (ClassNotFoundException e) {
            throw new BizException("加载编译后的实体类失败: " + fullClassName);
        }
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /**
     * Java 源码文件对象
     */
    private static class JavaSourceObject extends SimpleJavaFileObject {
        private final String code;

        JavaSourceObject(String className, String code) {
            super(URI.create("string:///" + className.replace('.', '/') + Kind.SOURCE.extension),
                    Kind.SOURCE);
            this.code = code;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return code;
        }
    }

    /**
     * 内存输出的 JavaFileManager
     */
    private static class MemoryFileManager extends ForwardingJavaFileManager<StandardJavaFileManager> {
        private final Map<String, byte[]> classBytes = new HashMap<>();

        MemoryFileManager(StandardJavaFileManager fileManager) {
            super(fileManager);
        }

        public Map<String, byte[]> getClassBytes() {
            return classBytes;
        }

        @Override
        public JavaFileObject getJavaFileForOutput(Location location, String className,
                                                   JavaFileObject.Kind kind, FileObject sibling) {
            return new MemoryClassObject(className, classBytes);
        }
    }

    /**
     * 内存字节码输出对象
     */
    private static class MemoryClassObject extends SimpleJavaFileObject {
        private final String className;
        private final Map<String, byte[]> target;

        MemoryClassObject(String className, Map<String, byte[]> target) {
            super(URI.create("mem:///" + className.replace('.', '/') + Kind.CLASS.extension),
                    Kind.CLASS);
            this.className = className;
            this.target = target;
        }

        @Override
        public OutputStream openOutputStream() {
            return new ByteArrayOutputStream() {
                @Override
                public void close() {
                    target.put(className, this.toByteArray());
                }
            };
        }
    }

    /**
     * 自定义 ClassLoader，从内存字节码加载
     */
    private static class MemoryClassLoader extends ClassLoader {
        private final Map<String, byte[]> classBytes;

        MemoryClassLoader(Map<String, byte[]> classBytes) {
            super(MemoryClassLoader.class.getClassLoader());
            this.classBytes = classBytes;
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            byte[] bytes = classBytes.get(name);
            if (bytes == null) {
                return super.loadClass(name);
            }
            return defineClass(name, bytes, 0, bytes.length);
        }
    }
}
