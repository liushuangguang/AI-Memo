package com.newtech.note.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan({"com.newtech.note.service","com.newtech.note.controller","com.newtech.note.repositories"})
public class ComponentConfiguration {
}
