package info.krumholz.tcgservice.interfaces.rest;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Date;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.common.io.BaseEncoding;

import info.krumholz.tcgservice.data.User;

@Controller
@RequestMapping("/api/users")
public class UsersController {

	// TODO: is required
	@Autowired(required = false)

	@RequestMapping(value = "", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<User> update(@RequestParam String modulusAsHexString,
			@RequestParam String exponentAsHexString,
			@RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date lastSeen,
			@DateTimeFormat(pattern = "yyyy-MM-dd") @RequestParam Date lastBooster) {
		try {
			byte[] modulusBytes = BaseEncoding.base16().decode(modulusAsHexString);
			byte[] exponentBytes = BaseEncoding.base16().decode(exponentAsHexString);

			RSAPublicKeySpec spec = new RSAPublicKeySpec(new BigInteger(1, modulusBytes),
					new BigInteger(1, exponentBytes));

			RSAPublicKey key;
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			key = (RSAPublicKey) keyFactory.generatePublic(spec);
			User user = new User(key, lastSeen, lastBooster);

			// TODO: store user
			return ResponseEntity.ok(user);
		} catch (Exception e) {
			HttpHeaders header = new HttpHeaders();
			header.add("faultMessage", e.getMessage());
			return new ResponseEntity<User>(header, HttpStatus.BAD_REQUEST);
		}
	}

	@RequestMapping(value = "", method = RequestMethod.GET)
	@ResponseBody
	public ResponseEntity<Set<User>> list() {
		// Set<User> values = userStore.getValues();
		return ResponseEntity.ok(null);
	}
}
