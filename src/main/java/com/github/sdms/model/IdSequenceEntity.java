package com.github.sdms.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "id_sequence")
@Getter
@Setter
public class IdSequenceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "type", unique = true, nullable = false)
    private String type;

    @Column(name = "current_value", nullable = false)
    private Long currentValue;
}

