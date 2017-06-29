package objects;


public class User {
    private String mName;
    private String mUsername;
    private String mInfo;
    private String mEnrollmentNo;
    private String mCsrfToken;
    private String mChanneliSessid;
    public User(String username, String csrftoken, String channeli_sessid){
        this.mUsername = username;
        this.mCsrfToken = csrftoken;
        this.mChanneliSessid = channeli_sessid;
    }
    public String getName(){
        return this.mName;
    }
    public String getInfo(){
        return this.mInfo;
    }
    public String getEnrollmentNo(){ return this.mEnrollmentNo; }
    public String getUsername(){ return this.mUsername; }
    public String getCsrfToken() { return this.mCsrfToken; }
    public String getChanneliSessid(){ return this.mChanneliSessid; }
    public void setName(String name){
        this.mName = name;
    }
    public void setInfo(String info){
        this.mInfo = info;
    }
    public void setEnrollmentNo(String enrollment_no){
        this.mEnrollmentNo = enrollment_no;
    }
}
