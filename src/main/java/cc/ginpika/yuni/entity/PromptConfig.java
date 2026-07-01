package cc.ginpika.yuni.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "prompt_config")
public class PromptConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String systemPrompt;
    
    private String personalityPrompt;
    
    private Boolean enabled;
}