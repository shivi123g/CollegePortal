import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.LocalDate;
import java.io.Serializable;
import java.util.*;

abstract class User implements Serializable{
    private String email;
    private String password;

    public User(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public boolean login(String email, String password) {
        return this.email.equals(email) && this.password.equals(password);
    }
}

class Student extends User {
    private String studentID;
    private int semester;
    private ArrayList<Course> registeredCourses;
    private HashMap<Course, String> grades;


    public Student(String email, String password, String studentID, int semester) {
        super(email, password);
        this.studentID = studentID;
        this.semester = semester;
        this.registeredCourses = new ArrayList<>();
        this.grades = new HashMap<>();
    }
    public int getSemester() {
        return semester;
    }
    public String getStudentID() {
        return studentID;
    }

    public void setSemester(int value) {
        this.semester = value;
    }

    // Method to set a grade for a specific course
    public void setGrades(String grade, Course course) {
        this.grades.put(course, grade);  // Adds/updates the grade for the specified course
    }

    public HashMap<Course, String> getGrades() {
        return this.grades;  // Returns the grades map
    }

    public void viewAvailableCourses(Course[] courses, int semester) {
        System.out.println("Available Courses for Semester " + semester + ":");
        boolean flag = false;
        for (Course course : courses) {
            if (semester == course.getSemester()) {
                flag = true;
                System.out.println(course);
            }
        }
        if(!flag){
            System.out.println("No records");

        }
    }

    public void registerCourse(Course course) throws CourseFullException{
        if (course.isFull()) {
            throw new CourseFullException("Cannot register. The course " + course.getCourseCode() + " is full.");
        }
            // proceed with registration

        if (!Application.getEnrolledStudents(course).contains(this)) {
            if (Application.enrolledCourses.containsKey(course)) {
                Application.enrolledCourses.get(course).add(this);
            } else {
                ArrayList<Student> studentList = new ArrayList<>();
                studentList.add(this);
                Application.enrolledCourses.put(course, studentList);
            }
        }

        int totalCredits = registeredCourses.stream().mapToInt(Course::getCredits).sum();
        if ((totalCredits + course.getCredits() <= 20) ) {
            registeredCourses.add(course);
            System.out.println("Registered for course: " + course.getTitle());
        } else {
            System.out.println("Cannot register for course: " + course.getTitle());
        }
    }

    private boolean prerequisitesMet(Course course) {
        for (String prereq : course.getPrerequisites()) {
            boolean met = false;
            for (Course completedCourse : registeredCourses) {
                if (completedCourse.getCourseCode().equals(prereq) && grades.containsKey(completedCourse)
                        && getGradePoints(grades.get(completedCourse)) >= 4.0) {
                    met = true;
                    break;
                }
            }
            if (!met) {
                System.out.println("Prerequisite not met for " + course.getTitle());
                return false;
            }
        }
        return true;
    }

    public void viewSchedule() {
        System.out.println("Your weekly schedule:");
        for (Course course : registeredCourses) {
            if (this.semester == course.getSemester()) {
                System.out.println(course.getSchedule());
            }
        }
    }

    public double trackProgress() {
        System.out.println("Grades:");
        for (Course course : registeredCourses) {
            System.out.println(course.getTitle() + ": " + grades.get(course));
        }

        double totalPoints = 0;
        int totalCredits = 0;
        for (Course course : registeredCourses) {
            int courseCredits = course.getCredits();
            totalCredits += courseCredits;
            totalPoints += getGradePoints(grades.get(course)) * courseCredits;
        }

        double gpa = (totalCredits > 0) ? totalPoints / totalCredits : 0.0;
        System.out.println("Current GPA: " + gpa);
        return gpa;
    }

    private double getGradePoints(String grade) {
        switch (grade) {
            case "A":
                return 10.0;
            case "A-":
                return 9.0;
            case "B":
                return 8.0;
            case "B-":
                return 7.0;
            case "C":
                return 6.0;
            case "C-":
                return 5.0;
            case "D":
                return 4.0;
            case "F":
                return 2.0;
            default:
                return 0.0;
        }
    }

    public void dropCourse(Course course) throws DropDeadlinePassedException {
        LocalDate dropDeadline = LocalDate.of(2024, 12, 1); // Set the course drop deadline
        LocalDate currentDate = LocalDate.now();

        if (currentDate.isAfter(dropDeadline)) {
            throw new DropDeadlinePassedException("Cannot drop course. Drop deadline for " + course.getTitle() + " has passed.");
        }

        if (registeredCourses.contains(course)) {
            registeredCourses.remove(course);
            System.out.println("Course dropped: " + course.getTitle());
        } else {
            System.out.println("You are not registered in this course.");
        }
    }

    public void giveFeedback(Course course, Scanner scanner) {
        System.out.println("Enter feedback type: 1 for Numeric (1-5), 2 for Textual");
        int feedbackType = scanner.nextInt();
        scanner.nextLine();  // Consume newline

        if (feedbackType == 1) {
            System.out.print("Enter numeric rating (1-5): ");
            int rating = scanner.nextInt();
            scanner.nextLine();  // Consume newline
            course.addFeedback(new Feedback<>(rating, this));
        } else if (feedbackType == 2) {
            System.out.print("Enter textual feedback: ");
            String textFeedback = scanner.nextLine();
            course.addFeedback(new Feedback<>(textFeedback, this));
        } else {
            System.out.println("Invalid feedback type.");
        }
    }


    public void submitComplaint(String info) {
        Complaint complaint = new Complaint(info);
        Application.complaints.add(complaint);
        System.out.println("Complaint submitted: " + info);
    }

}
class TeachingAssistant extends Student {

    // Constructor
    public TeachingAssistant(String email, String password, String studentID, int semester) {
        super(email, password,studentID,semester);
    }

    // Method to view grades of enrolled students
    public void viewStudentGrades(Course course) {
        System.out.println("Viewing student grades for course: " + course.getTitle());
        ArrayList<Student> enrolledStudents = Application.getEnrolledStudents(course);

        for (Student student : enrolledStudents) {
            HashMap<Course, String> grades = student.getGrades();  // Assuming a getGrades method in Student
            if (grades.containsKey(course)) {
                System.out.println("Student: " + student.getStudentID() + " - Grade: " + grades.get(course));
            } else {
                System.out.println("Student: " + student.getStudentID() + " - No grade assigned yet.");
            }
        }
    }


    public void assignGrades(Student student, Course course, String grade) {
        System.out.println("Assigning grade for student: " + student.getStudentID() + " in course: " + course.getTitle());
        if (Application.getEnrolledStudents(course).contains(student)) {
            student.getGrades().put(course, grade);  // Assuming grades is accessible via a method
            System.out.println("Grade assigned: " + grade);
        } else {
            System.out.println("Student is not enrolled in the course.");
        }
    }
}
class Professor extends User {
    private String professorID;

    public Professor(String email, String password, String professorID) {
        super(email, password);
        this.professorID = professorID;
    }

    public void viewAndUpdateCourse(Course course,int newCredits,String[] newPrerequisites, String newSchedule) {
        if (Application.courses.contains(course) && course.getProfessor().getProfessorID().equals(this.professorID)) {
            course.updateDetails( newCredits, newPrerequisites, newSchedule);
            System.out.println("Course details updated: " + course);
        } else {
            System.out.println("You are not authorized to update this course.");
        }
    }


    public void viewCourseFeedback(Course course) {
        course.viewFeedback();
    }

    public void viewEnrolledStudents(Course course) {
        System.out.println("Enrolled Students for course " + course.getTitle() + ":");
        for (Student student : Application.getEnrolledStudents(course)) {
            System.out.println("Student ID: " + student.getStudentID() + ", Email: " + student.getEmail());
        }
    }

    public String getProfessorID() {
        return professorID;
    }
}
class Feedback<T> implements Serializable {
    private T feedback;
    private Student student;

    public Feedback(T feedback, Student student) {
        this.feedback = feedback;
        this.student = student;
    }

    public T getFeedback() {
        return feedback;
    }

    public Student getStudent() {
        return student;
    }

    @Override
    public String toString() {
        return "Feedback from " + student.getStudentID() + ": " + feedback.toString();
    }
}
class Course implements Serializable{
    private String courseCode;
    private String title;
    private Professor professor;
    private int credits;
    private String[] prerequisites;
    private int semester;
    private String schedule;
    private int maxCapacity ;
    private ArrayList<Feedback<?>> feedbackList;


    public Course(String courseCode, String title, Professor professor, int credits, String[] prerequisites, int semester,
                  String schedule,int maxCapacity) {
        this.courseCode = courseCode;
        this.title = title;
        this.professor = professor;
        this.credits = credits;
        this.prerequisites = prerequisites;
        this.semester = semester;
        this.schedule = schedule;
        this.maxCapacity =  maxCapacity;
        this.feedbackList = new ArrayList<>();
    }

    public <T> void addFeedback(Feedback<T> feedback) {
        feedbackList.add(feedback);
    }
    public boolean isFull() {
        return Application.getEnrolledStudents(this).size() >= maxCapacity;
    }
    // View feedback for the course
    public void viewFeedback() {
        System.out.println("Feedback for " + courseCode + ":");
        for (Feedback<?> feedback : feedbackList) {
            System.out.println(feedback);
        }
    }


    public void updateDetails(  int newCredits, String[] newPrerequisites,String newSchedule) {
        this.credits = newCredits;
        this.prerequisites = newPrerequisites;
        this.schedule = newSchedule;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public String getTitle() {
        return title;
    }
    public int getMaxCapacity() {
        return maxCapacity;
    }
    public Professor getProfessor() {
        return professor;
    }

    public int getCredits() {
        return credits;
    }

    public String[] getPrerequisites() {
        return prerequisites;
    }

    public int getSemester() {
        return semester;
    }

    public String getSchedule() {
        return schedule;
    }
    public void setProfessor(Professor professor){
        this.professor = professor;

    }
    @Override
    public String toString() {
        return title + " (" + courseCode + ")";
    }
}

class Complaint implements Serializable{
    private String description;
    private String status;

    // Constructor
    public Complaint(String description) {
        this.description = description;
        this.status = "PENDING"; // Default status is PENDING
    }

    // Getter method for description
    public String getInfo() {
        return description;
    }

    // Setter method for status
    public void setStatus(String status) {
        this.status = status;
    }

    // Getter method for status (optional)
    public String getStatus() {
        return status;
    }
}
class Administrator extends User {
    public Administrator(String email, String password) {
        super(email, password);
    }

    public void addCourse(Course course) {
        Application.courses.add(course);
        System.out.println("Course added: " + course);
    }

    public void deleteCourse(Course course) {
        Application.courses.remove(course);
        System.out.println("Course removed: " + course);
    }

    public void updateStudentRecord(Student student, String field, Object value) {
        switch (field) {
            case "semester":
                student.setSemester((Integer) value);
                break;
            case "grades":
                student.setGrades((String) value,Application.courses.get(1));
                break;
            default:
                System.out.println("Invalid field.");
                break;
        }
        System.out.println("Student record updated: " + student);
    }

    public void assignProfessorToCourse(Professor professor, Course course) {
        course.setProfessor(professor);
        System.out.println("Professor assigned to course: " + course);
    }

    public void viewComplaints(ArrayList<Complaint> complaints) {
        System.out.println("Complaints:");
        for (Complaint complaint : complaints) {
            System.out.println("Complaint: " + complaint.getInfo() + ", Status: " + complaint.getStatus());
        }
    }

    public void updateComplaintStatus(Complaint complaint, String status) {
        complaint.setStatus(status);
        System.out.println("Complaint status updated to: " + status);
    }
}


class Application {

    public static HashMap<Course, ArrayList<Student>> enrolledCourses = new HashMap<>();
    public static ArrayList<User> users = new ArrayList<>();
    public static ArrayList<Course> courses = new ArrayList<>();
    public static ArrayList<Complaint> complaints = new ArrayList<>();

    public static void saveData() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("n_data.ser"))) {
            oos.writeObject(users);
            oos.writeObject(courses);
            oos.writeObject(complaints);
            oos.writeObject(enrolledCourses);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Course findCourseByTitle(String title) {
        for (Course course : courses) {
            if (course.getTitle().equalsIgnoreCase(title)) {
                return course;
            }
        }
        return null; // Course not found
    }

    private static Student findStudentByID(String id) {
        for (User user : users) {
            if (user instanceof Student && ((Student) user).getStudentID().equals(id)) {
                return (Student) user;
            }
        }
        return null; // Student not found
    }

    public static ArrayList<Student> getEnrolledStudents(Course course) {
        return enrolledCourses.getOrDefault(course, new ArrayList<>());
    }

    @SuppressWarnings("unchecked")
    public static void loadData() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream("n_data.ser"))) {
            users = (ArrayList<User>) ois.readObject();
            courses = (ArrayList<Course>) ois.readObject();
            complaints = (ArrayList<Complaint>) ois.readObject();
            enrolledCourses = (HashMap<Course, ArrayList<Student>>) ois.readObject();
        } catch (FileNotFoundException e) {
            System.out.println("Data file not found. Starting with fresh data.");
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static void setupData() {
        Student student1 = new Student("john@university.com", "password123", "S001", 1);
        Student student2 = new Student("jane@university.com", "password456", "S002", 1);
        Student student3 = new Student("sam@university.com", "password789", "S003", 2);



        // Adding students to the users list

        users.add(student1);
        users.add(student2);
        users.add(student3);
        Professor prof1 = new Professor("dr.smith@university.com", "pass123", "P001");
        Professor prof2 = new Professor("dr.jones@university.com", "pass456", "P002");
        users.add(prof1);
        users.add(prof2);
        Administrator ad1 = new Administrator("ad123@xyz.com", "pa1234");
        users.add(ad1);
        courses.add(new Course("CS101", "Intro to CS", prof1, 4, new String[]{}, 1, "Mon-Wed 10:00-11:30", 150));
        courses.add(new Course("CS102", "Data Structures", prof1, 4, new String[]{"CS101"}, 2, "Tue-Thu 12:00-1:30", 150));
        courses.add(new Course("CS201", "Algorithms", prof2, 4, new String[]{"CS102"}, 3, "Mon-Wed 10:00-11:30", 150));
        courses.add(new Course("CS202", "Operating Systems", prof2, 4, new String[]{"CS201"}, 2, "Tue-Thu 12:00-1:30", 150));
        courses.add(new Course("CS203", "Computer Networks", prof2, 2, new String[]{"CS201"}, 3, "Fri 10:00-12:00", 150));


    }

    public static void main(String[] args) {
        // Setup initial data
        loadData();
        File file = new File("n_data.ser");
        if (!file.exists()) {
            setupData();
            saveData();
        }

        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("1. Sign up as Student");
            System.out.println("2. Sign up as Professor");
            System.out.println("3. Login as Administrator");
            System.out.println("4. Login as Student");
            System.out.println("5. Login as Professor");
            System.out.println("6. Exit");
            System.out.print("Choose an option: ");
            int option = scanner.nextInt();
            scanner.nextLine();

            switch (option) {
                case 4:
                    loginAsStudent(scanner);
                    break;
                case 5:
                    loginAsProfessor(scanner);
                    break;
                case 3:
                    loginAsAdministrator(scanner);
                    break;
                case 1:
                    signUpAsStudent(scanner);
                    break;
                case 2:
                    signUpAsProfessor(scanner);
                    break;

                case 6:
                    saveData();
                    System.out.println("Exiting...");
                    return; // Ends the program
                default:
                    System.out.println("Invalid option. Try again.");
                    break;
            }
        }
    }


    private static void signUpAsStudent(Scanner scanner) {
        System.out.println("=== Sign Up as Student ===");
        System.out.print("Enter email: ");
        String email = scanner.nextLine();
        System.out.print("Enter password: ");
        String password = scanner.nextLine();
        System.out.print("Enter student ID: ");
        String studentID = scanner.nextLine();
        System.out.print("Enter current semester: ");
        int semester = scanner.nextInt();
        scanner.nextLine(); // consume newline

        // Create new student and add to users list
        Student newStudent = new Student(email, password, studentID, semester);
        users.add(newStudent);

        System.out.println("Student account created successfully!");
        saveData(); // Save the updated user list to file
    }

    private static void signUpAsProfessor(Scanner scanner) {
        System.out.println("=== Sign Up as Professor ===");
        System.out.print("Enter email: ");
        String email = scanner.nextLine();
        System.out.print("Enter password: ");
        String password = scanner.nextLine();
        System.out.print("Enter professor ID: ");
        String professorID = scanner.nextLine();

        // Create new professor and add to users list
        Professor newProfessor = new Professor(email, password, professorID);
        users.add(newProfessor);

        System.out.println("Professor account created successfully!");
        saveData(); // Save the updated user list to file
    }

    private static void loginAsStudent(Scanner scanner) {
        System.out.print("Enter email: ");
        String email = scanner.nextLine();
        System.out.print("Enter password: ");
        String password = scanner.nextLine();
        try {
            boolean found = false;
            for (User user : users) {
                if (user instanceof Student && user.login(email, password)) {
                    found = true;
                    Student student = (Student) user;
                    while (true) {
                        System.out.println("1. View available courses");
                        System.out.println("2. Register for a course");
                        System.out.println("3. View schedule");
                        System.out.println("4. Track progress");
                        System.out.println("5. Drop a course");
                        System.out.println("6. Submit a complaint");
                        System.out.println("7. Give feedback on a course");
                        System.out.println("8. TA");
                        System.out.println("9. Logout");
                        System.out.print("Choose an option: ");
                        int choose = scanner.nextInt();
                        scanner.nextLine(); // consume newline

                        switch (choose) {
                            case 1:
                                System.out.print("Enter semester number: ");
                                int choice = scanner.nextInt();
                                student.viewAvailableCourses(courses.toArray(new Course[0]), choice);
                                break;
                            case 2:
                                System.out.print("Enter course code: ");
                                String code = scanner.nextLine().trim();  // Get the course code from the user and trim whitespace

                                // Find the course in the available courses list
                                Course selectedCourse = null;
                                for (Course course : courses) {
                                    if (course.getCourseCode().equalsIgnoreCase(code)) {  // Compare ignoring case
                                        selectedCourse = course;  // If course found, store it
                                        break;  // Exit the loop
                                    }
                                }

                                // Check if the course exists and try to register the student
                                if (selectedCourse != null) {
                                    try{
                                        student.registerCourse(selectedCourse);
                                        saveData();

                                    }
                                    catch (CourseFullException e){
                                        System.out.println(e.getMessage());
                                    }
                                    // Attempt to register
                                } else {
                                    System.out.println("Course with code " + code + " not found.");  // If no course found with the code
                                }
                                break;
                            case 3:
                                student.viewSchedule();
                                break;
                            case 4:
                                student.trackProgress();
                                break;
                            case 5:
                                System.out.print("Enter course code: ");
                                String coded = scanner.nextLine().trim();  // Get the course code from the user and trim whitespace

                                // Find the course in the available courses list
                                Course selectedCoursed = null;
                                for (Course course : courses) {
                                    if (course.getCourseCode().equalsIgnoreCase(coded)) {  // Compare ignoring case
                                        selectedCoursed = course;  // If course found, store it
                                        break;  // Exit the loop
                                    }
                                }

                                // Check if the course exists and try to register the student
                                if (selectedCoursed != null) {
                                    try {
                                        student.dropCourse(selectedCoursed);
                                        saveData();// Attempt to register
                                    } catch (DropDeadlinePassedException e) {
                                        System.out.println(e.getMessage());  // Handle case where the course is full
                                    }
                                } else {
                                    System.out.println("Course with code " + coded + " not found.");  // If no course found with the code
                                }
                                break;

                            case 6:
                                System.out.print("Enter complaint: ");
                                String description = scanner.nextLine();
                                student.submitComplaint(description);
                                break;
                            case 7:
                                System.out.print("Enter course code: ");
                                String codef = scanner.nextLine().trim();  // Get the course code from the user and trim whitespace

                                // Find the course in the available courses list
                                Course selectedCoursef = null;
                                for (Course course : courses) {
                                    if (course.getCourseCode().equalsIgnoreCase(codef)) {  // Compare ignoring case
                                        selectedCoursef = course;  // If course found, store it
                                        break;  // Exit the loop
                                    }
                                }

                                // Check if the course exists and try to register the student
                                if (selectedCoursef != null) {
                                    student.giveFeedback(selectedCoursef, scanner);
                                    saveData();
                                } else {
                                    System.out.println("Course with code " + codef + " not found.");  // If no course found with the code
                                }
                                break;
                                // Example: Giving feedback on the first course in the list
                            case 8 :
                                Student stud = new TeachingAssistant(student.getEmail(), student.getPassword(), student.getStudentID(), student.getSemester());
                                if (stud instanceof TeachingAssistant) {
                                    TeachingAssistant ta = (TeachingAssistant) stud;

                                    // Start TA menu loop
                                    while (true) {
                                        System.out.println("=== Teaching Assistant Menu ===");
                                        System.out.println("1. View student grades");
                                        System.out.println("2. Assign grades to students");
                                        System.out.println("3. Logout as TA");
                                        System.out.print("Choose an option: ");
                                        int taChoice = scanner.nextInt();
                                        scanner.nextLine(); // Consume newline

                                        switch (taChoice) {
                                            case 1:
                                                // Viewing grades for a specific course
                                                System.out.print("Enter course title to view student grades: ");
                                                String courseTitle = scanner.nextLine();
                                                // Use helper method to find course
                                                Course courseToView = null;
                                                // Loop through the list of courses to find the matching course
                                                for (Course course : courses) {
                                                    if (course.getTitle().equalsIgnoreCase(courseTitle)) {
                                                        courseToView = course; // Course found
                                                        break;
                                                    }
                                                }

                                                if (courseToView != null) {
                                                    ta.viewStudentGrades(courseToView); // TA views student grades for the found course
                                                } else {
                                                    System.out.println("Course not found.");
                                                }
                                                break;

                                            case 2:
                                                // Assigning grades to a specific student
                                                System.out.print("Enter student ID to assign grade: ");
                                                String studentID = scanner.nextLine();
                                                // Use helper method to find student
                                                Student studentToGrade = null;
                                                // Loop through the list of students to find the matching student by ID
                                                for (User currentuser : users) { // Assuming 'users' contains all types of users (e.g., Student, Professor, etc.)
                                                    if (currentuser instanceof Student) { // Check if the user is a Student
                                                        Student studentcheck = (Student) currentuser; // Safely cast the user to a Student
                                                        if (studentcheck.getStudentID().equalsIgnoreCase(studentID)) {
                                                            studentToGrade = studentcheck; // Student found
                                                            break;
                                                        }
                                                    }
                                                }

                                                if (studentToGrade != null) {
                                                    System.out.print("Enter course title: ");
                                                    String courseTitleToAssign = scanner.nextLine();
                                                    Course courseToAssign = null;
                                                    // Loop through the courses list to find the course to assign a grade to
                                                    for (Course course : courses) {
                                                        if (course.getTitle().equalsIgnoreCase(courseTitleToAssign)) {
                                                            courseToAssign = course; // Course found
                                                            break;
                                                        }
                                                    }

                                                    if (courseToAssign != null) {
                                                        System.out.print("Enter grade to assign: ");
                                                        String grade = scanner.nextLine();
                                                        ta.assignGrades(studentToGrade, courseToAssign, grade); // Assign grade to student
                                                        System.out.println("Grade assigned successfully.");
                                                    } else {
                                                        System.out.println("Course not found.");
                                                    }
                                                } else {
                                                    System.out.println("Student not found.");
                                                }
                                                break;

                                            case 3:
                                                // Save data and log out as TA
                                                saveData();
                                                return; // Exit the TA menu

                                            default:
                                                System.out.println("Invalid option.");
                                        }
                                    }
                                }else{
                                    System.out.println("Student not found.");
                                }


                        // If we reach here, no valid TA was found
                            case 9:
                                saveData();
                                return; // Exit the loop and return to the main menu
                            default:
                                System.out.println("Invalid option.");
                        }
                    }
                }
            }
            if (!found) {
                throw new InvalidLoginException("Invalid email or password. Please try again.");
            }

        } catch (InvalidLoginException e) {
            System.out.println(e.getMessage());
        }

    }



    private static void loginAsProfessor(Scanner scanner) {
        System.out.print("Enter email: ");
        String email = scanner.nextLine();
        System.out.print("Enter password: ");
        String password = scanner.nextLine();
        try {
            boolean found = false;
            for (User user : users) {
                found = true;
                if (user instanceof Professor && user.login(email, password)) {
                    Professor professor = (Professor) user;
                    while (true) {
                        System.out.println("1. Update course");
                        System.out.println("2. View enrolled students");
                        System.out.println("3. View course feedback");
                        System.out.println("4. Logout");
                        System.out.print("Choose an option: ");
                        int choose = scanner.nextInt();
                        scanner.nextLine(); // consume newline

                        switch (choose) {
                            case 1:
                                System.out.print("Enter course code: ");
                                String courseCode = scanner.nextLine();
                                Course courseToUpdate = null;

                                // Search for the course by course code
                                for (Course course : courses) {
                                    if (course.getCourseCode().equalsIgnoreCase(courseCode)) {
                                        courseToUpdate = course;
                                        break;
                                    }
                                }
                                System.out.print("Enter new credits: ");
                                int credits = scanner.nextInt();
                                scanner.nextLine(); // consume newline
                                System.out.print("Enter new schedule: ");
                                String schedule = scanner.nextLine();
                                scanner.nextLine();
                                System.out.print("Enter new prerequisites (comma-separated): ");
                                String[] prerequisites = scanner.nextLine().split(","); // Process as array

                                professor.viewAndUpdateCourse(courseToUpdate, credits, prerequisites, schedule);
                                break;
                            case 2:
                                System.out.print("Enter course code: ");
                                String courseCodev = scanner.nextLine();
                                Course courseToUpdatev = null;

                                // Search for the course by course code
                                for (Course course : courses) {
                                    if (course.getCourseCode().equalsIgnoreCase(courseCodev)) {
                                        courseToUpdatev = course;
                                        break;
                                    }
                                }
                                professor.viewEnrolledStudents(courseToUpdatev); // Example registration
                                break;
                            case 3:
                                // Example: View feedback on the first course
                                System.out.print("Enter course code: ");
                                String courseCodef = scanner.nextLine();
                                Course courseToUpdatef = null;

                                // Search for the course by course code
                                for (Course course : courses) {
                                    if (course.getCourseCode().equalsIgnoreCase(courseCodef)) {
                                        courseToUpdatef = course;
                                        break;
                                    }
                                }
                                professor.viewCourseFeedback(courseToUpdatef);
                                break;
                            case 4:
                                saveData();
                                return; // Exit the loop and return to the main menu
                            default:
                                System.out.println("Invalid option.");
                        }
                    }
                }
            }
            if (!found) {
                throw new InvalidLoginException("Invalid email or password. Please try again.");
            }
        } catch (InvalidLoginException e) {
            System.out.println(e.getMessage());
        }


    }

    private static void loginAsAdministrator(Scanner scanner) {
        System.out.print("Enter email: ");
        String email = scanner.nextLine();
        System.out.print("Enter password: ");
        String password = scanner.nextLine();

        for (User user : users) {
            if (user instanceof Administrator && user.login(email, password)) {
                Administrator admin = (Administrator) user;
                while (true) {
                    System.out.println("1. Add course");
                    System.out.println("2. Delete course");
                    System.out.println("3. Update student record");
                    System.out.println("4. Assign professor to course");
                    System.out.println("5. View complaints");
                    System.out.println("6. Update complaint status");
                    System.out.println("7. Logout");
                    int choose = scanner.nextInt();
                    scanner.nextLine(); // consume newline

                    switch (choose) {
                        case 1:
                            System.out.print("Enter course code: ");
                            String courseC = scanner.nextLine();
                            System.out.print("Enter course title: ");
                            String courseTitle = scanner.nextLine();
                            System.out.print("Enter new credits: ");
                            int credits = scanner.nextInt();
                            System.out.print("Enter new schedule: ");
                            String schedule = scanner.nextLine();
                            System.out.print("Enter new prerequisites (comma-separated): ");
                            String[] prerequisites = scanner.nextLine().split(",");
                            System.out.print("Enter semester: ");
                            int semester = scanner.nextInt();
                            System.out.print("Enter capacity: ");
                            int capacity = scanner.nextInt();
                            Course addon = new Course(courseC, courseTitle, (Professor) users.get(4),credits,prerequisites,semester,schedule,capacity );


                            admin.addCourse(addon);
                            break;
                        case 2:
                            System.out.print("Enter course code: ");
                            String courseCode = scanner.nextLine();
                            Course coursedelete = null;

                            // Search for the course by course code
                            for (Course course : courses) {
                                if (course.getCourseCode().equalsIgnoreCase(courseCode)) {
                                    coursedelete = course;
                                    break;
                                }
                            }
                            admin.deleteCourse(coursedelete);
                            break;
                        case 3:
                            // Example: Update student record
                            admin.updateStudentRecord((Student) users.get(0), "semester", 3);
                            break;
                        case 4:
                            admin.assignProfessorToCourse((Professor) users.get(4), courses.get(0));
                            break;
                        case 5:
                            admin.viewComplaints(complaints);
                            break;
                        case 6:
                            System.out.print("Enter complaint number: ");
                            int choice = scanner.nextInt();
                            admin.updateComplaintStatus(complaints.get(choice - 1), "RESOLVED");
                            break;
                        case 7:
                            saveData();
                            return; // Exit the loop and return to the main menu
                        default:
                            System.out.println("Invalid option.");
                    }
                }
            }
        }
        System.out.println("Invalid login credentials.");


    }
}