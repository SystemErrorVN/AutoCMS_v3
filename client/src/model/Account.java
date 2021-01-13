
package model;

import java.util.Arrays;

/**
 * @name AutoCMS v3.0.0 OB1
 * @created 03/06/2020
 * @author ThienDepZaii - SystemError
 * @Facebook /ThienDz.SystemError
 * @Gmail ThienDz.DEV@gmail.com
 */

public class Account{
    
    private String cookie;
    private String csrfToken;
    private String userName;
    private String userId;
    private String email;
    
    private Course[] courses;
    
    public Account() {
    }

    public Account(String cookie, String csrfToken, String userName, String userId, String email, Course[] courses) {
        this.cookie = cookie;
        this.csrfToken = csrfToken;
        this.userName = userName;
        this.userId = userId;
        this.email = email;
        this.courses = courses;
    }

    
    public String getCookie() {
        return cookie;
    }

    public void setCookie(String cookie) {
        this.cookie = cookie;
    }

    public String getCsrfToken() {
        return csrfToken;
    }

    public void setCsrfToken(String csrfToken) {
        this.csrfToken = csrfToken;
    }

    public String getUserName() {
        return userName;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getEmail() {
        return userName+"@fpt.edu.vn";
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Course[] getCourses() {
        return courses;
    }

    public void setCourses(Course[] courses) {
        this.courses = courses;
    }

    @Override
    public String toString() {
        return "Account{" + "cookie=" + cookie + ", csrfToken=" + csrfToken + ", userName=" + userName + ", userId=" + userId + ", email=" + email + ", courses=" + Arrays.toString(courses) + '}';
    }

    

}