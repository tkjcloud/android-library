/* tkjcloud Android Library is available under MIT license
 *   Copyright (C) 2015 tkjcloud Inc.
 *   Copyright (C) 2012  Bartek Przybylski
 *   
 *   Permission is hereby granted, free of charge, to any person obtaining a copy
 *   of this software and associated documentation files (the "Software"), to deal
 *   in the Software without restriction, including without limitation the rights
 *   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *   copies of the Software, and to permit persons to whom the Software is
 *   furnished to do so, subject to the following conditions:
 *   
 *   The above copyright notice and this permission notice shall be included in
 *   all copies or substantial portions of the Software.
 *   
 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
 *   EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *   MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND 
 *   NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS 
 *   BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN 
 *   ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
 *   CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *   THE SOFTWARE.
 *
 */

package com.tkjcloud.android.lib.common.accounts;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountsException;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.net.Uri;

import com.tkjcloud.android.lib.common.tkjcloudClient;
import com.tkjcloud.android.lib.common.tkjcloudCredentials;
import com.tkjcloud.android.lib.common.tkjcloudCredentialsFactory;
import com.tkjcloud.android.lib.common.utils.Log_OC;
import com.tkjcloud.android.lib.resources.status.tkjcloudVersion;

import org.apache.commons.httpclient.Cookie;

import java.io.IOException;

public class AccountUtils {
	
	private static final String TAG = AccountUtils.class.getSimpleName();
	
    public static final String WEBDAV_PATH_1_2 = "/webdav/tkjcloud.php";
    public static final String WEBDAV_PATH_2_0 = "/files/webdav.php";
    public static final String WEBDAV_PATH_4_0 = "/remote.php/webdav";
    public static final String WEBDAV_PATH_9_0 = "/remote.php/dav";
    public static final String ODAV_PATH = "/remote.php/odav";
    private static final String SAML_SSO_PATH = "/remote.php/webdav";
    public static final String CARDDAV_PATH_2_0 = "/apps/contacts/carddav.php";
    public static final String CARDDAV_PATH_4_0 = "/remote/carddav.php";
    public static final String STATUS_PATH = "/status.php";

    /**
     * Returns the proper URL path to access the WebDAV interface of an tkjcloud server,
     * according to its version and the authorization method used.
     * 
     * @param	version         	Version of tkjcloud server.
     * @param 	supportsOAuth		If true, access with OAuth 2 authorization is considered. 
     * @param 	supportsSamlSso		If true, and supportsOAuth is false, access with SAML-based single-sign-on is considered.
     * @return 						WebDAV path for given OC version, null if OC version unknown
     */
    public static String getWebdavPath(tkjcloudVersion version, boolean supportsOAuth, boolean supportsSamlSso) {
        if (version != null) {
            if (supportsOAuth) {
                return ODAV_PATH;
            }
            if (supportsSamlSso) {
                return SAML_SSO_PATH;
            }

            if (version.compareTo(tkjcloudVersion.tkjcloud_v4) >= 0)
                return WEBDAV_PATH_4_0;
            if (version.compareTo(tkjcloudVersion.tkjcloud_v3) >= 0
                    || version.compareTo(tkjcloudVersion.tkjcloud_v2) >= 0)
                return WEBDAV_PATH_2_0;
            if (version.compareTo(tkjcloudVersion.tkjcloud_v1) >= 0)
                return WEBDAV_PATH_1_2;
        }
        return null;
    }
    
    /**
     * Constructs full url to host and webdav resource basing on host version
     * 
     * @deprecated 		To be removed in release 1.0. 
     * 
     * @param context
     * @param account
     * @return url or null on failure
     * @throws AccountNotFoundException     When 'account' is unknown for the AccountManager
     */
    @Deprecated
    public static String constructFullURLForAccount(Context context, Account account) throws AccountNotFoundException {
        AccountManager ama = AccountManager.get(context);
        String baseurl = ama.getUserData(account, Constants.KEY_OC_BASE_URL);
        String version  = ama.getUserData(account, Constants.KEY_OC_VERSION);
        boolean supportsOAuth = (ama.getUserData(account, Constants.KEY_SUPPORTS_OAUTH2) != null);
        boolean supportsSamlSso = (ama.getUserData(account, Constants.KEY_SUPPORTS_SAML_WEB_SSO) != null);
        tkjcloudVersion ver = new tkjcloudVersion(version);
        String webdavpath = getWebdavPath(ver, supportsOAuth, supportsSamlSso);

        if (baseurl == null || webdavpath == null) 
            throw new AccountNotFoundException(account, "Account not found", null);
        
        return baseurl + webdavpath;
    }
    
    /**
     * Extracts url server from the account
     * 
     * @deprecated 	This method will be removed in version 1.0.
     *  			Use {@link #getBaseUrlForAccount(Context, Account)}
     *  		 	instead.   
     * 
     * @param context
     * @param account
     * @return url server or null on failure
     * @throws AccountNotFoundException     When 'account' is unknown for the AccountManager
     */
    @Deprecated
    public static String constructBasicURLForAccount(Context context, Account account) 
    		throws AccountNotFoundException {
    	return getBaseUrlForAccount(context, account);
    }

    /**
     * Extracts url server from the account
     * @param context
     * @param account
     * @return url server or null on failure
     * @throws AccountNotFoundException     When 'account' is unknown for the AccountManager
     */
    public static String getBaseUrlForAccount(Context context, Account account) 
    		throws AccountNotFoundException {
        AccountManager ama = AccountManager.get(context.getApplicationContext());
        String baseurl = ama.getUserData(account, Constants.KEY_OC_BASE_URL);
        
        if (baseurl == null ) 
            throw new AccountNotFoundException(account, "Account not found", null);
        
        return baseurl;
    }


    /**
     * Get the username corresponding to an OC account.
     *
     * @param account   An OC account
     * @return          Username for the given account, extracted from the account.name
     */
    public static String getUsernameForAccount(Account account) {
        String username = null;
        try {
            username = account.name.substring(0, account.name.lastIndexOf('@'));
        } catch (Exception e) {
            Log_OC.e(TAG, "Couldn't get a username for the given account", e);
        }
        return username;
    }

	/**
	 * Get the stored server version corresponding to an OC account.
	 *
	 * @param account An OC account
	 * @param context Application context
	 * @return Version of the OC server, according to last check
	 */
	public static tkjcloudVersion getServerVersionForAccount(Account account, Context context) {
		AccountManager ama = AccountManager.get(context);
		tkjcloudVersion version = null;
		try {
			String versionString = ama.getUserData(account, Constants.KEY_OC_VERSION);
			version = new tkjcloudVersion(versionString);

		} catch (Exception e) {
			Log_OC.e(TAG, "Couldn't get a the server version for an account", e);
		}
		return version;
	}

    /**
     * 
     * @return
     * @throws IOException 
     * @throws AuthenticatorException 
     * @throws OperationCanceledException 
     */
	public static tkjcloudCredentials getCredentialsForAccount(Context context, Account account) 
			throws OperationCanceledException, AuthenticatorException, IOException {
		
		tkjcloudCredentials credentials = null;
        AccountManager am = AccountManager.get(context);
        
        boolean isOauth2 = am.getUserData(
        		account, 
        		AccountUtils.Constants.KEY_SUPPORTS_OAUTH2) != null;
        
        boolean isSamlSso = am.getUserData(
        		account, 
        		AccountUtils.Constants.KEY_SUPPORTS_SAML_WEB_SSO) != null;

        String username = AccountUtils.getUsernameForAccount(account);
		String ocVersion = am.getUserData(account, Constants.KEY_OC_VERSION);

		tkjcloudVersion version;
		if (ocVersion == null) {
			// set to oldest supported version
			version = tkjcloudVersion.nextcloud_10;
		} else {
			version = new tkjcloudVersion(ocVersion);
		}

        if (isOauth2) {    
            String accessToken = am.blockingGetAuthToken(
            		account, 
            		AccountTypeUtils.getAuthTokenTypeAccessToken(account.type), 
            		false);
            
            credentials = tkjcloudCredentialsFactory.newBearerCredentials(accessToken);
        
        } else if (isSamlSso) {
            String accessToken = am.blockingGetAuthToken(
            		account, 
            		AccountTypeUtils.getAuthTokenTypeSamlSessionCookie(account.type), 
            		false);
            
            credentials = tkjcloudCredentialsFactory.newSamlSsoCredentials(username, accessToken);

        } else {
            String password = am.blockingGetAuthToken(
					account,
					AccountTypeUtils.getAuthTokenTypePass(account.type),
					false);

			credentials = tkjcloudCredentialsFactory.newBasicCredentials(username, password,
					version.isPreemptiveAuthenticationPreferred());
		}
        
        return credentials;
        
	}

	
    public static String buildAccountNameOld(Uri serverBaseUrl, String username) {
    	if (serverBaseUrl.getScheme() == null) {
    		serverBaseUrl = Uri.parse("https://" + serverBaseUrl.toString()); 
    	}
        String accountName = username + "@" + serverBaseUrl.getHost();
        if (serverBaseUrl.getPort() >= 0) {
            accountName += ":" + serverBaseUrl.getPort();
        }
        return accountName;
    }

    public static String buildAccountName(Uri serverBaseUrl, String username) {
    	if (serverBaseUrl.getScheme() == null) {
    		serverBaseUrl = Uri.parse("https://" + serverBaseUrl.toString());
    	}

        // Remove http:// or https://
        String url = serverBaseUrl.toString();
        if (url.contains("://")) {
            url = url.substring(serverBaseUrl.toString().indexOf("://") + 3);
        }
        String accountName = username + "@" + url;

        return accountName;
    }

	public static void saveClient(tkjcloudClient client, Account savedAccount, Context context) {

		// Account Manager
		AccountManager ac = AccountManager.get(context.getApplicationContext());

		if (client != null) {
			String cookiesString = client.getCookiesString();
			if (!"".equals(cookiesString)) {
				ac.setUserData(savedAccount, Constants.KEY_COOKIES, cookiesString); 
				// Log_OC.d(TAG, "Saving Cookies: "+ cookiesString );
			}
		}

	}

	
  /**
  * Restore the client cookies
  * @param account
  * @param client 
  * @param context
  */
	public static void restoreCookies(Account account, tkjcloudClient client, Context context) {

		Log_OC.d(TAG, "Restoring cookies for " + account.name);

		// Account Manager
		AccountManager am = AccountManager.get(context.getApplicationContext());

		Uri serverUri = (client.getBaseUri() != null)? client.getBaseUri() : client.getWebdavUri();

		String cookiesString = null;
		try {
			cookiesString = am.getUserData(account, Constants.KEY_COOKIES);
		} catch (SecurityException e) {
			Log_OC.e(TAG, e.getMessage());
		}

		if (cookiesString !=null) {
			String[] cookies = cookiesString.split(";");
			if (cookies.length > 0) {
				for (int i=0; i< cookies.length; i++) {
					Cookie cookie = new Cookie();
					int equalPos = cookies[i].indexOf('=');
					cookie.setName(cookies[i].substring(0, equalPos));
					cookie.setValue(cookies[i].substring(equalPos + 1));
					cookie.setDomain(serverUri.getHost());	// VERY IMPORTANT 
					cookie.setPath(serverUri.getPath());	// VERY IMPORTANT

					client.getState().addCookie(cookie);
				}
			}
		}
	}
	
	/**
	 * Restore the client cookies from accountName
	 * @param accountName
	 * @param client
	 * @param context
	 */
	public static void restoreCookies(String accountName, tkjcloudClient client, Context context) {
		Log_OC.d(TAG, "Restoring cookies for " + accountName);

		// Account Manager
		AccountManager am = AccountManager.get(context.getApplicationContext());
		
		// Get account
		Account account = null;
		Account accounts[] = am.getAccounts();
		for (Account a : accounts) {
			if (a.name.equals(accountName)) {
				account = a;
				break;
			}
		}
		
		// Restoring cookies
		if (account != null) {
			restoreCookies(account, client, context);
		}
	}
	
    public static class AccountNotFoundException extends AccountsException {
        
		/** Generated - should be refreshed every time the class changes!! */
		private static final long serialVersionUID = -1684392454798508693L;
        
        private Account mFailedAccount; 
                
        public AccountNotFoundException(Account failedAccount, String message, Throwable cause) {
            super(message, cause);
            mFailedAccount = failedAccount;
        }
        
        public Account getFailedAccount() {
            return mFailedAccount;
        }
    }


	public static class Constants {
	    /**
	     * Value under this key should handle path to webdav php script. Will be
	     * removed and usage should be replaced by combining
	     * {@link com.tkjcloud.android.authentication.AuthenticatorActivity.KEY_OC_BASE_URL} and
	     * {@link com.tkjcloud.android.lib.resources.status.tkjcloudVersion}
	     * 
	     * @deprecated
	     */
	    public static final String KEY_OC_URL = "oc_url";
	    /**
	     * Version should be 3 numbers separated by dot so it can be parsed by
	     * {@link com.tkjcloud.android.lib.resources.status.tkjcloudVersion}
	     */
	    public static final String KEY_OC_VERSION = "oc_version";
	    /**
	     * Base url should point to tkjcloud installation without trailing / ie:
	     * http://server/path or https://tkjcloud.server
	     */
	    public static final String KEY_OC_BASE_URL = "oc_base_url";
	    /**
	     * Flag signaling if the tkjcloud server can be accessed with OAuth2 access tokens.
	     */
	    public static final String KEY_SUPPORTS_OAUTH2 = "oc_supports_oauth2";
	    /**
	     * Flag signaling if the tkjcloud server can be accessed with session cookies from SAML-based web single-sign-on.
	     */
	    public static final String KEY_SUPPORTS_SAML_WEB_SSO = "oc_supports_saml_web_sso";
	    /**
	    * Flag signaling if the tkjcloud server supports Share API"
        * @deprecated
        */
	    public static final String KEY_SUPPORTS_SHARE_API = "oc_supports_share_api";
	    /**
	     * OC account cookies
	     */
	    public static final String KEY_COOKIES = "oc_account_cookies";

        /**
         * OC account version
         */
        public static final String KEY_OC_ACCOUNT_VERSION = "oc_account_version";

		/**
		 * User's display name
		 */
		public static final String KEY_DISPLAY_NAME = "oc_display_name";

		/**
		 * User ID
		 */
		public static final String KEY_USER_ID = "oc_id";
	}

}
