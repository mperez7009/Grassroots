/*
 *  Copyright (c) 2014, Parse, LLC. All rights reserved.
 *
 *  You are hereby granted a non-exclusive, worldwide, royalty-free license to use,
 *  copy, modify, and distribute this software in source code or binary form for use
 *  in connection with the web services and APIs provided by Parse.
 *
 *  As with any software that integrates with the Parse platform, your use of
 *  this software is subject to the Parse Terms of Service
 *  [https://www.parse.com/about/terms]. This copyright notice shall be
 *  included in all copies or substantial portions of the software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 *  FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 *  COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 *  IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 *  CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package com.parse.ui;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;
import com.parse.LogInCallback;
import com.parse.ParseException;
import com.parse.ParseFacebookUtils;
import com.parse.ParseUser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

/**
 * Fragment for the user login screen.
 */
public class ParseLoginFragment extends ParseLoginFragmentBase {

    public interface ParseLoginFragmentListener {
        public void onSignUpClicked(String username, String password);

        public void onLoginHelpClicked();

        public void onLoginSuccess();
    }

    private static final String LOG_TAG = "ParseLoginFragment";
    private static final String USER_OBJECT_NAME_FIELD = "name";

    private View parseLogin;
    private EditText usernameField;
    private EditText passwordField;
    private TextView parseLoginHelpButton;
    private Button parseLoginButton;
    private Button parseSignupButton;
    private Button facebookLoginButton;
    private Button twitterLoginButton;
    private ParseLoginFragmentListener loginFragmentListener;
    private ParseOnLoginSuccessListener onLoginSuccessListener;

    private ParseLoginConfig config;
    private static final float BITMAP_SCALE = 0.05f;
    private static final float BLUR_RADIUS = 7.5f;


    public static ParseLoginFragment newInstance(Bundle configOptions) {
        ParseLoginFragment loginFragment = new ParseLoginFragment();
        loginFragment.setArguments(configOptions);
        return loginFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent,
                             Bundle savedInstanceState) {
        config = ParseLoginConfig.fromBundle(getArguments(), getActivity());

        View v = inflater.inflate(R.layout.com_parse_ui_parse_login_fragment,
                parent, false);

        Bitmap result = blur(getContext(), BitmapFactory.decodeResource(getResources(),R.drawable.grassroots));
        Drawable drawable = new BitmapDrawable(result);
        v.setBackground(drawable);

        ImageView appLogo = (ImageView) v.findViewById(R.id.app_logo);
        parseLogin = v.findViewById(R.id.parse_login);
        usernameField = (EditText) v.findViewById(R.id.login_username_input);
        passwordField = (EditText) v.findViewById(R.id.login_password_input);
        parseLoginHelpButton = (Button) v.findViewById(R.id.parse_login_help);
        parseLoginButton = (Button) v.findViewById(R.id.parse_login_button);
        parseSignupButton = (Button) v.findViewById(R.id.parse_signup_button);
        facebookLoginButton = (Button) v.findViewById(R.id.facebook_login);
        twitterLoginButton = (Button) v.findViewById(R.id.twitter_login);

        if (appLogo != null && config.getAppLogo() != null) {
            appLogo.setImageResource(config.getAppLogo());
        }
        if (allowParseLoginAndSignup()) {
            setUpParseLoginAndSignup();
        }
        if (allowFacebookLogin()) {
            setUpFacebookLogin();
        }
        if (allowTwitterLogin()) {
//      setUpTwitterLogin();
        }
        return v;
    }

    public static Bitmap blur(Context ctx, Bitmap image) {
        int width = Math.round(image.getWidth() * BITMAP_SCALE);
        int height = Math.round(image.getHeight() * BITMAP_SCALE);

        Bitmap inputBitmap = Bitmap.createScaledBitmap(image, width, height, false);
        Bitmap outputBitmap = Bitmap.createBitmap(inputBitmap);

        RenderScript rs = RenderScript.create(ctx);
        ScriptIntrinsicBlur theIntrinsic = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
        Allocation tmpIn = Allocation.createFromBitmap(rs, inputBitmap);
        Allocation tmpOut = Allocation.createFromBitmap(rs, outputBitmap);
        theIntrinsic.setRadius(BLUR_RADIUS);
        theIntrinsic.setInput(tmpIn);
        theIntrinsic.forEach(tmpOut);
        tmpOut.copyTo(outputBitmap);

        return outputBitmap;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        final Activity activity = getActivity();
        if (activity instanceof ParseLoginFragmentListener) {
            loginFragmentListener = (ParseLoginFragmentListener) activity;
        } else {
            throw new IllegalArgumentException(
                    "Activity must implemement ParseLoginFragmentListener");
        }

        if (activity instanceof ParseOnLoginSuccessListener) {
            onLoginSuccessListener = (ParseOnLoginSuccessListener) activity;
        } else {
            throw new IllegalArgumentException(
                    "Activity must implemement ParseOnLoginSuccessListener");
        }

        if (activity instanceof ParseOnLoadingListener) {
            onLoadingListener = (ParseOnLoadingListener) activity;
        } else {
            throw new IllegalArgumentException(
                    "Activity must implemement ParseOnLoadingListener");
        }
    }

    @Override
    protected String getLogTag() {
        return LOG_TAG;
    }

    private void setUpParseLoginAndSignup() {
        parseLogin.setVisibility(View.VISIBLE);

        if (config.isParseLoginEmailAsUsername()) {
            usernameField.setHint(R.string.com_parse_ui_email_input_hint);
            usernameField.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        }

        if (config.getParseLoginButtonText() != null) {
            parseLoginButton.setText(config.getParseLoginButtonText());
        }

        parseLoginButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = usernameField.getText().toString();
                String password = passwordField.getText().toString();

                if (username.length() == 0) {
                    if (config.isParseLoginEmailAsUsername()) {
                        showToast(R.string.com_parse_ui_no_email_toast);
                    } else {
                        showToast(R.string.com_parse_ui_no_username_toast);
                    }
                } else if (password.length() == 0) {
                    showToast(R.string.com_parse_ui_no_password_toast);
                } else {
                    loadingStart(true);
                    ParseUser.logInInBackground(username, password, new LogInCallback() {
                        @Override
                        public void done(ParseUser user, ParseException e) {
                            if (isActivityDestroyed()) {
                                return;
                            }

                            if (user != null) {
                                loadingFinish();
                                loginSuccess();
                            } else {
                                loadingFinish();
                                if (e != null) {
                                    debugLog(getString(R.string.com_parse_ui_login_warning_parse_login_failed) +
                                            e.toString());
                                    if (e.getCode() == ParseException.OBJECT_NOT_FOUND) {
                                        if (config.getParseLoginInvalidCredentialsToastText() != null) {
                                            showToast(config.getParseLoginInvalidCredentialsToastText());
                                        } else {
                                            showToast(R.string.com_parse_ui_parse_login_invalid_credentials_toast);
                                        }
                                        passwordField.selectAll();
                                        passwordField.requestFocus();
                                    } else {
                                        showToast(R.string.com_parse_ui_parse_login_failed_unknown_toast);
                                    }
                                }
                            }
                        }
                    });
                }
            }
        });

        if (config.getParseSignupButtonText() != null) {
            parseSignupButton.setText(config.getParseSignupButtonText());
        }

        parseSignupButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = usernameField.getText().toString();
                String password = passwordField.getText().toString();

                loginFragmentListener.onSignUpClicked(username, password);
            }
        });

        if (config.getParseLoginHelpText() != null) {
            parseLoginHelpButton.setText(config.getParseLoginHelpText());
        }

        parseLoginHelpButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                loginFragmentListener.onLoginHelpClicked();
            }
        });
    }


    private LogInCallback facebookLoginCallbackV4 = new LogInCallback() {
        @Override
        public void done(ParseUser user, ParseException e) {
            if (isActivityDestroyed()) {
                return;
            }
            if (user == null) {
                loadingFinish();
                if (e != null) {
                    showToast(R.string.com_parse_ui_facebook_login_failed_toast);
                    debugLog(getString(R.string.com_parse_ui_login_warning_facebook_login_failed) +
                            e.toString());
                }
            } else if (user.isNew()) {
                Log.d("user", "new");
                Bundle parameters = new Bundle();
                parameters.putString("fields", "email,name,picture.height(961)");
                new GraphRequest(
                        AccessToken.getCurrentAccessToken(),
                        "/me",
                        parameters,
                        HttpMethod.GET,
                        new GraphRequest.Callback() {
                            public void onCompleted(GraphResponse response) {
                                Log.d("user response", "received");
                                ParseUser parseUser = ParseUser.getCurrentUser();

         /* handle the result */
                                try {

                                    String email = response.getJSONObject().getString("email");
                                    Log.d("user", email);
                                    String name = response.getJSONObject().getString("name");
                                    Log.d("user", name);
                                    String id = response.getJSONObject().getString("id");
                                    Log.d("user", id);

                                    JSONObject picture = response.getJSONObject().getJSONObject("picture");
                                    JSONObject data = picture.getJSONObject("data");

                                    //  Returns a 50x50 profile picture
                                    String profileImgUrl = data.getString("url");
                                    Log.d("user", profileImgUrl);

                                    parseUser.setEmail(email);
                                    parseUser.put("name", name);
                                    parseUser.put("profileImgUrl", profileImgUrl);
                                    parseUser.put("fbId", id);
                                    parseUser.put("totalPoints", 0);
                                    parseUser.put("joinDate", new Date());
                                    parseUser.put("friends", new JSONArray());
                                    JSONObject resourceData = new JSONObject();
                                    resourceData.put("emissions", 0.0);
                                    resourceData.put("fuel", 0.0);
                                    resourceData.put("water", 0.0);
                                    resourceData.put("trees", 0.0);

                                    parseUser.put("resourceData", resourceData);

                                    parseUser.saveInBackground();
                                    Log.d("user", "loaded");
                                    loginSuccess();


                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                ).executeAsync();

            } else {
                loginSuccess();
            }
        }
    };

    private void setUpFacebookLogin() {
        facebookLoginButton.setVisibility(View.VISIBLE);

        if (config.getFacebookLoginButtonText() != null) {
            facebookLoginButton.setText(config.getFacebookLoginButtonText());
        }

        facebookLoginButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                loadingStart(false); // Facebook login pop-up already has a spinner
                if (config.isFacebookLoginNeedPublishPermissions()) {
                    ParseFacebookUtils.logInWithPublishPermissionsInBackground(getActivity(),
                            config.getFacebookLoginPermissions(), facebookLoginCallbackV4);
                } else {
                    ParseFacebookUtils.logInWithReadPermissionsInBackground(getActivity(),
                            config.getFacebookLoginPermissions(), facebookLoginCallbackV4);
                }
            }
        });
    }

    private boolean allowParseLoginAndSignup() {
        if (!config.isParseLoginEnabled()) {
            return false;
        }

        if (usernameField == null) {
            debugLog(R.string.com_parse_ui_login_warning_layout_missing_username_field);
        }
        if (passwordField == null) {
            debugLog(R.string.com_parse_ui_login_warning_layout_missing_password_field);
        }
        if (parseLoginButton == null) {
            debugLog(R.string.com_parse_ui_login_warning_layout_missing_login_button);
        }
        if (parseSignupButton == null) {
            debugLog(R.string.com_parse_ui_login_warning_layout_missing_signup_button);
        }
        if (parseLoginHelpButton == null) {
            debugLog(R.string.com_parse_ui_login_warning_layout_missing_login_help_button);
        }

        boolean result = (usernameField != null) && (passwordField != null)
                && (parseLoginButton != null) && (parseSignupButton != null)
                && (parseLoginHelpButton != null);

        if (!result) {
            debugLog(R.string.com_parse_ui_login_warning_disabled_username_password_login);
        }
        return result;
    }

    private boolean allowFacebookLogin() {
        if (!config.isFacebookLoginEnabled()) {
            return false;
        }

        if (facebookLoginButton == null) {
            debugLog(R.string.com_parse_ui_login_warning_disabled_facebook_login);
            return false;
        } else {
            return true;
        }
    }

    private boolean allowTwitterLogin() {
        if (!config.isTwitterLoginEnabled()) {
            return false;
        }

        if (twitterLoginButton == null) {
            debugLog(R.string.com_parse_ui_login_warning_disabled_twitter_login);
            return false;
        } else {
            return true;
        }
    }

    private void loginSuccess() {
        onLoginSuccessListener.onLoginSuccess();
        Log.d("login", "success");
    }

}