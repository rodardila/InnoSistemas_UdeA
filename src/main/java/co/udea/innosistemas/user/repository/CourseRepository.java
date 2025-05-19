package co.udea.innosistemas.user.repository;

import co.udea.innosistemas.user.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, Long> {
}