package com.example.reactivespringnative;

import java.io.Serializable;

import org.springframework.data.annotation.Id;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class Subject implements Serializable{
    @Id
    private Integer id;
    private String name;
    private String city;

}