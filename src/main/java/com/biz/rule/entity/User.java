package com.biz.rule.entity;

import java.io.Serializable;

/**
 * 预置实体：用户
 */
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private Integer age;
    /** VIP / NORMAL */
    private String userLevel;
    /** NORMAL / FROZEN */
    private String status;

    public User() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    public String getUserLevel() { return userLevel; }
    public void setUserLevel(String userLevel) { this.userLevel = userLevel; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return "User{id=" + id + ", name='" + name + "', level=" + userLevel + "}";
    }
}
