package cc.ginpika.yuni.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "messages")
public class MessageEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String sessionId;
    
    private String role;
    
    @Column(length = 65535)
    private String content;
    
    @Column(length = 65535)
    private String rawResponse;
    
    private LocalDateTime createdAt;
}
