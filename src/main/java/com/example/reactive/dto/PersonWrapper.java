package com.example.reactive.dto;

import lombok.Data;

import java.util.List;

@Data
public class PersonWrapper {

  private List<Person> persons;
}
