package com.channeli.noticeboard;

/**
 * Created by thewisebro on 29/10/17.
 */

public interface Constants {
    public static final String DOMAIN_URL="people.iitr.ernet.in";
    public static final String HOST_URL="http://people.iitr.ernet.in/";
    public static final String NOTICES_URL = HOST_URL+"notices/";
    public static final String LOGIN_URL = HOST_URL+"login/";
    public static final String PEOPLE_SEARCH_URL = HOST_URL+"peoplesearch/";
    public static final String PHOTO_URL = HOST_URL+"photo/";
    public static final String READ_NOTICES_URL = NOTICES_URL+"read_notice_list/";
    public static final String STARRED_NOTICES_URL = NOTICES_URL+"star_notice_list/";
    public static final String READ_STAR_NOTICE_URL = NOTICES_URL+"read_star_notice/";
    public static final String NOTICE_URL = NOTICES_URL + "get_notice/";
    public static final String LOGOUT_URL = HOST_URL+"logout/";
    public static final String PREFS_NAME = "MyPrefsFile";
    public static final String CHANNELI_SESSID="CHANNELI_SESSID",CSRF_TOKEN="csrftoken";
}
