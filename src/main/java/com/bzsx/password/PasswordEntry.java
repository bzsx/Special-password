package com.bzsx.password;

public class PasswordEntry {
    private long id;
    private String name;
    private String account;
    private String encryptedPassword;
    private long createdAt;
    private long updatedAt;
    private long sortOrder; // 0=未置顶, >0=置顶(值为时间戳)
    private String website; // 关联的网站或App包名，用于自动填充

    public PasswordEntry() {}

    public PasswordEntry(String name, String account, String encryptedPassword) {
        this.name = name;
        this.account = account;
        this.encryptedPassword = encryptedPassword;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        this.sortOrder = 0;
        this.website = "";
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAccount() { return account; }
    public void setAccount(String account) { this.account = account; }
    public String getEncryptedPassword() { return encryptedPassword; }
    public void setEncryptedPassword(String encryptedPassword) { this.encryptedPassword = encryptedPassword; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
    public long getSortOrder() { return sortOrder; }
    public void setSortOrder(long sortOrder) { this.sortOrder = sortOrder; }
    public boolean isPinned() { return sortOrder > 0; }
    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }
}
