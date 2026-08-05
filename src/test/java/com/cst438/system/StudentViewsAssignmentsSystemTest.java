package com.cst438.system;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StudentViewsAssignmentsSystemTest {

    private WebDriver driver;
    private WebDriverWait wait;

    private static final String FRONTEND_URL = "http://localhost:5173/";

    @BeforeEach
    void setUp() {
        driver = new ChromeDriver();
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        driver.manage().window().maximize();
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    void studentViewsNewAssignmentWithBlankScore() {

        loginAsInstructor();

        selectFall2025();

        openCst599Assignments();

        // TODO: Generate a randomized assignment title and create the assignment.
        // TODO: Verify the instructor sees the new assignment.
        // TODO: Log out and log in as samb@csumb.edu.
        // TODO: Open the student assignments page and select Fall 2025.
        // TODO: Verify CST599 and the new assignment appear.
        // TODO: Verify the assignment score is blank.
    }

    private void loginAsInstructor() {
        driver.get(FRONTEND_URL);

        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id("email")
        ));

        driver.findElement(By.id("email"))
                .sendKeys("ted@csumb.edu");

        driver.findElement(By.id("password"))
                .sendKeys("ted2025");

        driver.findElement(By.id("loginButton")).click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//h1[contains(text(), 'Instructor Home')]")
        ));
    }

    private void selectFall2025() {

        /*
         * Locate the two term inputs associated with the
         * "Get Sections" button.
         *
         * The first input is year and the second is semester.
         */
        WebElement getSectionsButton =
                wait.until(ExpectedConditions.elementToBeClickable(
                        By.xpath("//button[normalize-space()='Get Sections']")
                ));

        List<WebElement> termInputs =
                driver.findElements(By.xpath(
                        "//button[normalize-space()='Get Sections']" +
                                "/preceding-sibling::input"
                ));

        assertEquals(
                2,
                termInputs.size(),
                "Expected year and semester input fields"
        );

        WebElement yearInput = termInputs.get(0);
        WebElement semesterInput = termInputs.get(1);

        yearInput.clear();
        yearInput.sendKeys("2025");

        semesterInput.clear();
        semesterInput.sendKeys("Fall");

        getSectionsButton.click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//tr[td[normalize-space()='cst599']]")
        ));
    }

    private void openCst599Assignments() {

        WebElement assignmentsLink =
                wait.until(ExpectedConditions.elementToBeClickable(
                        By.xpath(
                                "//tr[td[normalize-space()='cst599']]" +
                                        "//a[normalize-space()='Assignments']"
                        )
                ));

        assignmentsLink.click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath(
                        "//h3[contains(normalize-space(), " +
                                "'Assignments for cst599')]"
                )
        ));
    }
}
