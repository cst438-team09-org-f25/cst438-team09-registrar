package com.cst438.controller;

import com.cst438.domain.*;
import com.cst438.dto.EnrollmentDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StudentControllerUnitTest {

    private EnrollmentRepository enrollmentRepository;
    private UserRepository userRepository;
    private StudentController controller;
    private Principal principal;
    private User student;

    @BeforeEach
    void setUp() {
        enrollmentRepository = mock(EnrollmentRepository.class);
        userRepository = mock(UserRepository.class);
        controller = new StudentController(enrollmentRepository, userRepository);

        principal = mock(Principal.class);
        student = mock(User.class);

        when(principal.getName()).thenReturn("student@csumb.edu");
        when(userRepository.findByEmail("student@csumb.edu")).thenReturn(student);
        when(student.getId()).thenReturn(10);
        when(student.getName()).thenReturn("Test Student");
        when(student.getEmail()).thenReturn("student@csumb.edu");
    }

    @Test
    void getScheduleReturnsEnrollmentDTOs() {
        Enrollment enrollment = createEnrollment(101, "A", "cst438",
                "Software Engineering", 4, 1, 5001, 2026, "Fall");

        when(enrollmentRepository.findByYearAndSemesterOrderByCourseId(
                2026, "Fall", 10)).thenReturn(List.of(enrollment));

        List<EnrollmentDTO> result =
                controller.getSchedule(2026, "Fall", principal);

        assertEquals(1, result.size());
        EnrollmentDTO dto = result.get(0);
        assertEquals(101, dto.enrollmentId());
        assertEquals("A", dto.grade());
        assertEquals(10, dto.studentId());
        assertEquals("Test Student", dto.name());
        assertEquals("student@csumb.edu", dto.email());
        assertEquals("cst438", dto.courseId());
        assertEquals("Software Engineering", dto.title());
        assertEquals(1, dto.sectionId());
        assertEquals(5001, dto.sectionNo());
        assertEquals(2026, dto.year());
        assertEquals("Fall", dto.semester());

        verify(userRepository).findByEmail("student@csumb.edu");
        verify(enrollmentRepository)
                .findByYearAndSemesterOrderByCourseId(2026, "Fall", 10);
    }

    @Test
    void getTranscriptReturnsEnrollments() {
        Enrollment first = createEnrollment(101, "A", "cst363",
                "Database Systems", 4, 1, 5001, 2025, "Fall");
        Enrollment second = createEnrollment(102, "B", "cst438",
                "Software Engineering", 4, 2, 5002, 2026, "Spring");

        when(enrollmentRepository.findEnrollmentsByStudentIdOrderByTermId(10))
                .thenReturn(List.of(first, second));

        List<EnrollmentDTO> result = controller.getTranscript(principal);

        assertEquals(2, result.size());
        assertEquals("cst363", result.get(0).courseId());
        assertEquals("cst438", result.get(1).courseId());

        verify(enrollmentRepository)
                .findEnrollmentsByStudentIdOrderByTermId(10);
    }

    @Test
    void getScheduleRejectsUnknownStudent() {
        when(userRepository.findByEmail("student@csumb.edu")).thenReturn(null);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> controller.getSchedule(2026, "Fall", principal));

        assertEquals(404, exception.getStatusCode().value());
        assertEquals("student not found", exception.getReason());
        verifyNoInteractions(enrollmentRepository);
    }

    @Test
    void getTranscriptRejectsMissingPrincipal() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> controller.getTranscript(null));

        assertEquals(401, exception.getStatusCode().value());
        assertEquals("student is not authenticated", exception.getReason());
        verifyNoInteractions(userRepository, enrollmentRepository);
    }

    private Enrollment createEnrollment(
            int enrollmentId, String grade, String courseId, String title,
            int credits, int sectionId, int sectionNo, int year,
            String semester) {

        Enrollment enrollment = mock(Enrollment.class);
        Section section = mock(Section.class);
        Course course = mock(Course.class);
        Term term = mock(Term.class);

        when(enrollment.getEnrollmentId()).thenReturn(enrollmentId);
        when(enrollment.getGrade()).thenReturn(grade);
        when(enrollment.getStudent()).thenReturn(student);
        when(enrollment.getSection()).thenReturn(section);

        when(section.getCourse()).thenReturn(course);
        when(section.getTerm()).thenReturn(term);
        when(section.getSectionId()).thenReturn(sectionId);
        when(section.getSectionNo()).thenReturn(sectionNo);
        when(section.getBuilding()).thenReturn("BIT");
        when(section.getRoom()).thenReturn("104");
        when(section.getTimes()).thenReturn("MW 10:00-11:50");

        when(course.getCourseId()).thenReturn(courseId);
        when(course.getTitle()).thenReturn(title);
        when(course.getCredits()).thenReturn(credits);

        when(term.getYear()).thenReturn(year);
        when(term.getSemester()).thenReturn(semester);

        return enrollment;
    }
}
