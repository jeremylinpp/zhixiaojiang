package com.zhixiaojiang.model.vo;

/** AttachmentDigest 投影。 */
public class AttachmentDigest {
    private Long id;
    private String sha256;
    private String originalName;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSha256() { return sha256; }
    public void setSha256(String sha256) { this.sha256 = sha256; }
    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }
}
