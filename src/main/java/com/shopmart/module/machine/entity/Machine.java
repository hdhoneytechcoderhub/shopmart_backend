package com.shopmart.module.machine.entity;

import com.shopmart.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter @Setter @NoArgsConstructor
@Entity
@Table(name = "machines")
public class Machine extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "model_number")
    private String modelNumber;

    private String brand;

    @Column(length = 2000)
    private String description;

    @ElementCollection
    @CollectionTable(name = "machine_images", joinColumns = @JoinColumn(name = "machine_id"))
    @Column(name = "image_url")
    private List<String> images = new ArrayList<>();

    /** Soft delete flag: deleted machines stay in the DB but are hidden from queries. */
    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;
}
