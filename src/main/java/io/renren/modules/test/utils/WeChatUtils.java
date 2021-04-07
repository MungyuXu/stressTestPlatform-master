package io.renren.modules.test.utils;

import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public class WeChatUtils {

    private RestTemplate restTemplate = new RestTemplate();
    private String accessToken;
    private static final Logger log = LoggerFactory.getLogger("WeChatUtils.class");

    /**
    * 获取accessToken
    */
    public String getAccessToken() throws JSONException {
        String corpid = "ww3e4fc9b495ff0fd8";
        String corpsecret = "3fO_54UZ-OZHwPP5T4JH8arqB_aFmF-TXT4xoSNwNU0";
        JSONObject param = new JSONObject();
        param.put("corpid", corpid);
        param.put("corpsecret", corpsecret);
        String getAccessTokenUri = "https://qyapi.weixin.qq.com/cgi-bin/gettoken";
        HttpEntity<JSONObject> formEntity = new HttpEntity<JSONObject>(param);
        ResponseEntity responseEntity = restTemplate.postForEntity(getAccessTokenUri, formEntity, JSONObject.class);
        JSONObject resBody = new JSONObject();
        if (responseEntity.getStatusCode().is2xxSuccessful()) {
            resBody = (JSONObject) (responseEntity.getBody());
        }
        accessToken = resBody.getString("access_token");
        return accessToken;
    }

    /**
     * 获取部门列表
     */
    public JSONArray getDepartmentList(String accessToken) throws JSONException {
        String url = "https://qyapi.weixin.qq.com/cgi-bin/department/list?access_token=" + accessToken + "";
        ResponseEntity responseEntity = restTemplate.postForEntity(url, null, JSONObject.class);

        JSONArray jsonArray = new JSONArray();
        if (responseEntity.getStatusCode().is2xxSuccessful()) {
            jsonArray = ((JSONObject) responseEntity.getBody()).getJSONArray("department");
        }

        return jsonArray;
    }

    /**
     * 获取指定部门信息
     */
    public Integer getSpecifiedDepartmentId(String accessToken, String departmentName) throws JSONException {
        int specifiedDepartmentId = 0;
        JSONArray departmentArray = getDepartmentList(accessToken);
        for (int i = 0; i < departmentArray.size(); i ++) {
            if (departmentArray.getJSONObject(i).getString("name").equalsIgnoreCase(departmentName)) {
                specifiedDepartmentId = departmentArray.getJSONObject(i).getInt("id");
                break;
            }
        }

        return specifiedDepartmentId;
    }

    /**
     * 获取用户列表
     */
    public JSONArray getUserList(String accessToken, int departmentId) throws JSONException {
        String url = "https://qyapi.weixin.qq.com/cgi-bin/user/list?access_token="+ accessToken +"&department_id="+
                departmentId +"&fetch_child=1";
        ResponseEntity responseEntity = restTemplate.postForEntity(url, null, JSONObject.class);

        JSONArray jsonArray = new JSONArray();
        if (responseEntity.getStatusCode().is2xxSuccessful()) {
            jsonArray = ((JSONObject) responseEntity.getBody()).getJSONArray("userlist");
        }

        return jsonArray;
    }

    /**
     * 获取指定用户userid
     */
    public Integer getSpecifiedUserId(String userEmail) throws JSONException {
        String accessToken = getAccessToken();
        Integer userId = null;
        String departmentName = "科技研发中心";
        Integer specifiedDepartmentId = getSpecifiedDepartmentId(accessToken, departmentName);
        JSONArray userList = getUserList(accessToken, specifiedDepartmentId);

        for (int i = 0; i < userList.size(); i++) {
            if (userList.getJSONObject(i).getString("email").equalsIgnoreCase(userEmail)) {
                userId = userList.getJSONObject(i).getInt("userid");
                break;
            }
        }

        return userId;
    }

    /**
     *发送压测结束消息给压测发起人
     **/
    public ResponseEntity<JSONObject> sendMessage(String email, String msg) throws JSONException  {
        JSONObject textContent = new JSONObject();
        textContent.put("content", msg);

        JSONObject reqBody = new JSONObject();
        reqBody.put("msgtype", "text");
        reqBody.put("safe", "0");
        reqBody.put("agentid", 1000201);
        reqBody.put("touser", getSpecifiedUserId(email));
        reqBody.put("text", textContent);

        String url = "https://qyapi.weixin.qq.com/cgi-bin/message/send?access_token="+ accessToken +"";

        HttpEntity<JSONObject> formEntity = new HttpEntity<>(reqBody);
        ResponseEntity responseEntity = restTemplate.postForEntity(url, formEntity, JSONObject.class);

        if (!responseEntity.getStatusCode().is2xxSuccessful()) {
            log.error("send wechat notification failed due to: " + responseEntity.getBody());
        }
        return responseEntity;
    }
}
