package cc.ginpika.yuni.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "sessions")
public class SessionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    @Column(unique = true)
    private String sessionId;
    
    private String status;
    
    @Column(length = 500)
    private String summary;
}
