package auto.solution;

import auto.getquiz.BuildQuiz;
import auto.getquiz.Exception.BuildQuizException;
import auto.solution.exception.SolutionException;
import util.Utilities;
import util.Combination;
import util.Console;
import util.Permutation;
import function.exception.InputException;
import java.io.IOException;
import java.util.ArrayList;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import model.Account;
import model.Course;
import model.Quiz;
import model.QuizQuestion;
import request.HttpRequest;
import request.support.HttpRequestHeader;

/**
 * @author ThienDepZaii - SystemError
 * @Facebook /ThienDz.SystemError
 * @Gmail ThienDz.DEV@gmail.com
 */
public class CMSSolution {

    private static final int TIME_SLEEP_SOLUTION = 60000;

    private Account cmsAccount;
    private Course course;
    private Quiz quiz;

    private ArrayList<ArrayList<Integer>> alInt;
    private double scorePresent;

    private boolean isUsing;

    private int status = -1;

    public CMSSolution() {
    }

    public CMSSolution(Account cmsAccount, Course course, Quiz quiz) {
        this.cmsAccount = cmsAccount;
        this.course = course;
        this.quiz = quiz;
    }

    public Account getCmsAccount() {
        return cmsAccount;
    }

    public void setCmsAccount(Account cmsAccount) {
        this.cmsAccount = cmsAccount;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public Quiz getQuiz() {
        return quiz;
    }

    public void setQuiz(Quiz quiz) {
        this.quiz = quiz;
    }

    public double getScorePresent() {
        return scorePresent;
    }

    public int getStatus() {
        return status;
    }

    public void solution() throws SolutionException {
        if (isUsing) {
            return;
        }
        isUsing = !isUsing;
        status = 2;
        scorePresent = quiz.getScore();
        //đã đủ điểm
        if (quiz.getScore() == quiz.getScorePossible() || isDoneQuiz(quiz)) {
            status = 1;
            return;
        }
        quiz = resetQuizQuestion(quiz);
        final String urlPost = buildURLPost();
        double solutionScore = 0;
        long timeTick = 0;
        do {
            if (Utilities.getCurrentMilis() - timeTick > 60000) {
                try {
                    //
                    String jsonResponse = httpRequestSolution(urlPost, buildParamPost());
                    Console.debug(jsonResponse);
                    //
                    Object o = JSONValue.parse(jsonResponse);
                    JSONObject jsonObj = (JSONObject) o;
                    solutionScore = Double.parseDouble(jsonObj.get("current_score").toString());
                    quiz = updateStatusQuizQuestion(jsonObj.get("contents").toString(), quiz);
                    Console.debug(quiz);
                    //
                    scorePresent = solutionScore;
                    timeTick = Utilities.getCurrentMilis();
                } catch (Exception e) {
                    status = 0;
                    throw new SolutionException(e.toString());
                }
            }
            Utilities.sleep(100);
        } while (!isDoneQuiz(quiz));
        status = 1;
    }

    //tạo parampost cho request
    private String buildParamPost() throws SolutionException {
        QuizQuestion quizQuestion[] = quiz.getQuizQuestion();
        StringBuilder sb = new StringBuilder();
        //ghép các parampost từ quizQuestion, thành paramPost Full
        for (int i = 0; i < quizQuestion.length; i++) {
            //câu hỏi này là câu hỏi tự luận thì bỏ qua
            if (quizQuestion[i].getListValue() == null) {
                continue;
            }
            String ans = setValue(quizQuestion[i]);
            sb.append(ans).append("&");
            if (!quizQuestion[i].isCorrect()) { //hoàn thành rồi thì bỏ qua tăng test
                quiz.getQuizQuestion()[i].setTestCount(quiz.getQuizQuestion()[i].getTestCount() + 1);
            }
            quiz.getQuizQuestion()[i].setSelectValue(ans); // set đáp án
        }
        return makeUpValue(sb.toString());
    }

    //convert quizQuestion sang paramPost, tự động ++ giá trị tiếp theo cho quizQUestion
    private String setValue(QuizQuestion quizQuestion) throws SolutionException {
        //đã hoàn thành thì chỉ lấy getAnswer()
        if (quizQuestion.isCorrect()) {
            return quizQuestion.getSelectValue();
        }
        // đây là kiểu multichoice
        if (quizQuestion.isMultiChoice()) {
            //tạo mới biến global alInt chứa danh sách tổ hợp chập k của n phần tử
            try {
                if (quizQuestion.getType().equals("text")) {
                    alInt = new Permutation(quizQuestion.getAmountInput(), quizQuestion.getListValue().length).getResult();
                } else {
                    alInt = new Combination(2, quizQuestion.getListValue().length, true).getResult();
                }
            } catch (InputException e) {
                throw new SolutionException("setValue Input Permutation or Combination Error!");
            }
            StringBuilder value = new StringBuilder();
            int index = quizQuestion.getTestCount();
            //nếu vượt quá index tổ hợp
            if (index >= alInt.size()) {
                throw new SolutionException("setValue ArrayIndexOutOfBound alInt!");
            }
            //nếu là text: định dạng key=value1,value2...
            if (quizQuestion.getType().equals("text")) {
                value.append(quizQuestion.getKey()).append("=");
            }
            alInt.get(index).forEach((i) -> {
                if (quizQuestion.getType().equals("text")) {
                    // kiểu text chỉ việc append value1,value2...
                    value.append(Utilities.URLEncoder(quizQuestion.getListValue()[i])).append("%2C");
                } else {
                    // kiểu checkbox => key[]=value1&key[]=value2.....
                    value.append(Utilities.URLEncoder(quizQuestion.getKey())).append("=").append(Utilities.URLEncoder(quizQuestion.getListValue()[i])).append("&");
                }
            });
            //xóa kí tự nối cuối và return quizQuestion
            return makeUpValue(value.toString());
        } else { // đây là kiểu chọn 1 đáp án
            String choice[] = quizQuestion.getListValue();
            //định dạng: key=value
            String res = Utilities.URLEncoder(quizQuestion.getKey()) + "=" + Utilities.URLEncoder(choice[quizQuestion.getTestCount()]) + "&";
            return makeUpValue(res);
        }
    }

    //kiểm tra giá trị đầu vào và setCorrect lại cho mỗi quizQuestion
    private static Quiz updateStatusQuizQuestion(String htmlResp, Quiz quiz) throws SolutionException, IOException, BuildQuizException {
        BuildQuiz buildQuiz = new BuildQuiz();
        buildQuiz.setHtmlResponse(htmlResp);
        buildQuiz.setGetStatus(true);
        buildQuiz.buildQuizQuestion();
        QuizQuestion[] quizResults = buildQuiz.getQuiz().getQuizQuestion();
        if (!compareKeyQuizQuestion(quiz.getQuizQuestion(), quizResults)) {
            throw new SolutionException("updateStatusQuizQuestion QuizResult != QuizStandard!");
        }
        for (int i = 0; i < quizResults.length; i++) {
            quiz.getQuizQuestion()[i].setCorrect(quizResults[i].isCorrect());
        }
        return quiz;
    }

    private static boolean compareKeyQuizQuestion(QuizQuestion[] quizQuestionsOne, QuizQuestion[] quizQuestionsTwo) {
        if (quizQuestionsOne.length == quizQuestionsTwo.length) {
            for (int i = 0; i < quizQuestionsOne.length; i++) {
                if (!quizQuestionsOne[i].getKey().equals(quizQuestionsTwo[i].getKey())) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private String httpRequestSolution(String url, String paramPost) throws IOException {
        HttpRequestHeader httpRequestHeader = new HttpRequestHeader();
        httpRequestHeader.add("cookie", cmsAccount.getCookie());
        httpRequestHeader.add("X-CSRFToken", cmsAccount.getCsrfToken());
        httpRequestHeader.add("Referer", quiz.getUrl());
        httpRequestHeader.add("Accept", "application/json, text/javascript, */*; q=0.01");
        HttpRequest httpRequest = new HttpRequest(url, paramPost, httpRequestHeader);
        return httpRequest.getResponseHTML();
    }

    private static String makeUpValue(String value) {
        int len = value.length();
        if (value.endsWith("&")) {
            return value.substring(0, len - 1);
        }
        if (value.endsWith("%2C")) {
            return value.substring(0, len - 3);
        }
        return value;
    }

    private static boolean isDoneQuiz(Quiz quiz) {
        QuizQuestion quizQuestions[] = quiz.getQuizQuestion();
        int done = 0;
        for (QuizQuestion quizQuestion : quizQuestions) {
            if (quizQuestion.isCorrect() || quizQuestion.getListValue() == null) {
                done++;
            }
        }
        return done == quizQuestions.length;
    }

    private static Quiz resetQuizQuestion(Quiz quiz) {
        for (QuizQuestion quizQuestion : quiz.getQuizQuestion()) {
            quizQuestion.setCorrect(false);
        }
        return quiz;
    }

    private String buildURLPost() {
        String urlPost = "https://cms.poly.edu.vn/courses/%s/xblock/%s+type@problem+block@%s/handler/xmodule_handler/problem_check";
        return String.format(urlPost, course.getId(), course.getId().replace("course", "block"), quiz.getQuizQuestion()[0].getKey().split("_")[1]);
    }
}