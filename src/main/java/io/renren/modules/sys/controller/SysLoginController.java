package io.renren.modules.sys.controller;

import com.google.code.kaptcha.Constants;
import com.google.code.kaptcha.Producer;
import io.renren.common.utils.R;
import io.renren.common.utils.ShiroUtils;
import io.renren.common.validator.ValidatorUtils;
import io.renren.common.validator.group.AddGroup;
import io.renren.modules.sys.entity.SysUserEntity;
import io.renren.modules.sys.form.LoginForm;
import io.renren.modules.sys.service.SysConfigService;
import io.renren.modules.sys.service.SysUserService;
import io.renren.modules.sys.service.SysUserTokenService;
import org.apache.commons.io.IOUtils;
import org.apache.shiro.crypto.hash.Sha256Hash;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.*;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;

/**
 * 登录相关
 *
 * @author chenshun
 * @email sunlightcs@gmail.com
 * @date 2016年11月10日 下午1:15:31
 */
@RestController
public class SysLoginController extends AbstractController {
    @Autowired
    private Producer producer;
    @Autowired
    private SysUserService sysUserService;
    @Autowired
    private SysUserTokenService sysUserTokenService;
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private SysConfigService sysConfigService;

    /**
     * 验证码
     */
    @RequestMapping("captcha.jpg")
    public void captcha(HttpServletResponse response) throws ServletException, IOException {
        response.setHeader("Cache-Control", "no-store, no-cache");
        response.setContentType("image/jpeg");

        //生成文字验证码
        String text = producer.createText();
        //生成图片验证码
        BufferedImage image = producer.createImage(text);
        //保存到shiro session
        ShiroUtils.setSessionAttribute(Constants.KAPTCHA_SESSION_KEY, text);

        ServletOutputStream out = response.getOutputStream();
        ImageIO.write(image, "jpg", out);
        IOUtils.closeQuietly(out);
    }

    /**
     * 登录
     */
    @RequestMapping(value = "/sys/login", method = RequestMethod.POST)
    public Map<String, Object> login(@RequestBody LoginForm form) throws IOException, JSONException {
        //本项目已实现，前后端完全分离，但页面还是跟项目放在一起了，所以还是会依赖session
        //如果想把页面单独放到nginx里，实现前后端完全分离，则需要把验证码注释掉(因为不再依赖session了)
        String kaptcha = ShiroUtils.getKaptcha(Constants.KAPTCHA_SESSION_KEY);
        if (!form.getCaptcha().equalsIgnoreCase(kaptcha)) {
            return R.error("验证码不正确");
        }

        //用户信息
        SysUserEntity user = sysUserService.queryByUserName(form.getUsername());

        //调用运维提供的登陆接口
        if (sysConfigService.queryObject((long) 8).getStatus() != 0) { //判断是否启用运维提供的登陆服务
            if (!form.getUsername().equalsIgnoreCase("admin")) {

                ResponseEntity<String> responseEntity = login(form.getUsername(), form.getPassword());
                if (responseEntity.getStatusCode() != HttpStatus.valueOf(200)) {
                    return R.error("登陆失败");
                }

                JSONObject result;
                JSONObject resBody = new JSONObject(responseEntity.getBody());

                if (resBody.getInt("status") == 0) {
                    result = resBody.getJSONObject("result");
                } else {
                    return R.error(resBody.getInt("status"), resBody.getString("message"));
                }

                //如用户表中无用户信息，将用户信息插入数据库
                if (user == null) {
                    sysUserService.save(getUserEntity(result, form.getPassword()));
                    user = sysUserService.queryByUserName(form.getUsername());
                }

                //检查用户表中的密码是否与用户输入的一致，不一致则更新
                if (!user.getPassword().equals(new Sha256Hash(form.getPassword(), user.getSalt()).toHex())) {
                    sysUserService.update(getUserEntity(result, form.getPassword()));
                    user = sysUserService.queryByUserName(form.getUsername());
                }
            }
        }

        //账号锁定
        if (user.getStatus() == 0) {
            return R.error("账号已被锁定,请联系管理员");
        }

        //生成token，并保存到数据库
        R r = sysUserTokenService.createToken(user.getUserId());
        return r;
    }


    /**
     * 退出
     */
    @RequestMapping(value = "/sys/logout", method = RequestMethod.POST)
    public R logout() {
        sysUserTokenService.logout(getUserId());
        return R.ok();
    }

    //调用运维提供的登陆接口
    private ResponseEntity<String> login(String username, String password) throws JSONException {
        JSONObject requestBody = new JSONObject();
        String url = sysConfigService.getValue("INTERNAL_LOGIN_URL");
        requestBody.put("username", username);
        requestBody.put("password", password);
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(HttpHeaders.CONTENT_TYPE, "application/json");
        httpHeaders.add(HttpHeaders.ACCEPT_LANGUAGE, "zh-CN,zh;q=0.9");
        HttpEntity<String> entity = new HttpEntity<>(requestBody.toString(), httpHeaders);
        restTemplate.getMessageConverters().set(1, new StringHttpMessageConverter(StandardCharsets.UTF_8));
        return restTemplate.postForEntity(url, entity, String.class);
    }

    //根据登陆接口返回信心设置 user信息
    private SysUserEntity getUserEntity(JSONObject result, String password) throws JSONException {
        String username = result.getString("username");
        String phone = result.getString("iphone");
        String email = result.getString("email");

        SysUserEntity sysUserEntity = new SysUserEntity();
        sysUserEntity.setEmail(email);
        sysUserEntity.setMobile(phone);
        sysUserEntity.setPassword(password);
        sysUserEntity.setStatus(1);
        sysUserEntity.setUsername(username);
        sysUserEntity.setRoleIdList(Arrays.asList((long) 2));
        ValidatorUtils.validateEntity(sysUserEntity, AddGroup.class);
        sysUserEntity.setCreateUserId((long) 1);

        return sysUserEntity;
    }
}
