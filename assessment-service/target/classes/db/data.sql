-- =============================================================================
-- Assessment Service Seed Data
-- Assessments for lessons from the course-catalog data
-- =============================================================================

INSERT INTO assessments (id, lesson_id, course_id, title, description, type, questions_json, passing_score, max_score, time_limit_minutes, created_at, updated_at)
VALUES
    (1, 4, 1, 'Java Basics Quiz', 'Quiz covering Java fundamentals from the introductory module', 'QUIZ',
     '[{"q":"What is the entry point of a Java application?","options":["main()","start()","run()","init()"],"answer":0},{"q":"Which keyword is used to create a new object?","options":["create","new","init","object"],"answer":1}]',
     70, 100, 15, NOW(), NOW()),
    (2, 7, 2, 'Web Development Assignment', 'Practical assignment on building a responsive web page', 'ASSIGNMENT',
     '[{"q":"Build a responsive landing page using HTML, CSS, and JavaScript. Include a navigation bar, hero section, and footer.","type":"open-ended"}]',
     60, 100, NULL, NOW(), NOW()),
    (3, 15, 3, 'Data Science Midterm Exam', 'Comprehensive exam covering data analysis and visualization', 'QUIZ',
     '[{"q":"Which Python library is primarily used for data manipulation?","options":["NumPy","Pandas","Matplotlib","Scikit-learn"],"answer":1},{"q":"What does CSV stand for?","options":["Comma Separated Values","Common Standard Values","Computed String Variables","Central System Variables"],"answer":0}]',
     75, 100, 30, NOW(), NOW())
ON CONFLICT DO NOTHING;

INSERT INTO student_answers (id, assessment_id, student_id, enrollment_id, answers_json, score, passed, graded_at, submitted_at, created_at)
VALUES
    (1, 1, 1, 1, '[{"q":0,"a":0},{"q":1,"a":1}]', 85, true, NOW(), NOW(), NOW()),
    (2, 3, 2, 5, '[{"q":0,"a":1},{"q":1,"a":0}]', 92, true, NOW(), NOW(), NOW())
ON CONFLICT DO NOTHING;
