package com.clydrive.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class BreadcrumbItem {

    private Long id;
    private String name;
}
