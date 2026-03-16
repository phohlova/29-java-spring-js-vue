package org.example._9javaspringjsvue.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ReviewDTO {
    private Long id;
    private Long productId;
    private String userName;
    private Integer rating;
    private String commentText;
    private LocalDateTime createdAt;
    private Boolean deleted;
    private List<ReviewImageDTO> images;
}