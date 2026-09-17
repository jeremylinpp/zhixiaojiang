package com.zhixiaojiang.model.vo;

/** TaskAttachmentRow 投影。 */
public class TaskAttachmentRow {
    private Long id;
    private String originalName;
    private Long sizeBytes;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }
    public Long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(Long sizeBytes) { this.sizeBytes = sizeBytes; }
}
