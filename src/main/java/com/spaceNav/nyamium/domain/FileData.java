package com.spaceNav.nyamium.domain;

import com.spaceNav.nyamium.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileData extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fileName;

    private String keyName;  //fileKey
    private String fileUrl;

    private Long dataNum;

    @OneToMany(mappedBy = "fileData", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Data> dataList;
}
