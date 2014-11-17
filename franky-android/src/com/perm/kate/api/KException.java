package com.perm.kate.api;

@SuppressWarnings("serial")
public class KException extends Exception{
	public static final int ERROR_CAPTCHA_REQUIRED = 14;
	public static final int ERROR_FLOOD_CONTROL = 9;

	public static final int ERROR_CODE_INVALID_PARAMETERS = 100;
	public static final int ERROR_CODE_INVALID_CONFIRMATION_CODE = 1110;
	public static final int ERROR_CODE_REGISTRATION_IS_PENDING = 1003;
	public static final int ERROR_CODE_TRY_AFTER_5_SECONDS = 1112;
	public static final int ERROR_CODE_PHONE_IS_TAKEN = 1004;

    public KException(int code, String message){
        super(message);
        error_code=code;
    }

    public int error_code;
    //for captcha
    public String captcha_img;
    public String captcha_sid;
}
