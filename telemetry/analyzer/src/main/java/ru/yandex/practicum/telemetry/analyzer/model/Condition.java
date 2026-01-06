package ru.yandex.practicum.telemetry.analyzer.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "conditions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Condition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private ConditionType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation")
    private ConditionOperation operation;

    @Column(name = "value")
    private Integer value;
}