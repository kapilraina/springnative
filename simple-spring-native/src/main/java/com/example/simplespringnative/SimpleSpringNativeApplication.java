package com.example.simplespringnative;

import java.util.Arrays;
import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;


@SpringBootApplication
@RequiredArgsConstructor
@RestController
public class SimpleSpringNativeApplication {

	public static void main(String[] args) {
		SpringApplication.run(SimpleSpringNativeApplication.class, args);
	}

	private final SubjectRepo repo;
	

	@Bean
	public ApplicationRunner aler() {
		return args -> {

			Arrays.asList("Rum", "Pum", "Pam")
					.stream()
					.map(n -> new Subject(null, n, n + "'s city"))
					.map(repo::save)
					.forEach(s -> System.out.println(s));

		};
	}



	@GetMapping("/subjects")
	public Collection<Subject> getAll()
	{
		return repo.findAll();
	}

	
}
	
interface SubjectRepo extends JpaRepository<Subject, Integer> {

}



