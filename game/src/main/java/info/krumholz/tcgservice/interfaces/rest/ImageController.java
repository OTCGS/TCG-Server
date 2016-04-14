package info.krumholz.tcgservice.interfaces.rest;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.imgscalr.Scalr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import info.krumholz.tcgservice.data.Image;
import info.krumholz.tcgservice.storage.UUIDPojoStore;
import info.krumholz.tcgservice.utils.CommonlyUsedMethods;

@Controller
@ControllerAdvice
@RequestMapping("/images")
public class ImageController {

	private static final Logger logger = LoggerFactory.getLogger(ImageController.class);

	@Autowired
	@Qualifier("imageStorage")
	UUIDPojoStore<Image> storage;

	@Autowired(required = false)
	private HashSet<String> supportedImageTypes;

	public void setSupportedContentTypes(HashSet<String> supportedContentTypes) {
		this.supportedImageTypes = supportedContentTypes;
	}

	@PostConstruct
	public void init() {
		if (supportedImageTypes == null) {
			supportedImageTypes = new HashSet<String>();
			supportedImageTypes.add("image/jpeg");
			supportedImageTypes.add("image/png");
			supportedImageTypes.add("image/gif");
		}
	}

	@RequestMapping(value = "/new", method = RequestMethod.POST, produces = "application/json")
	public @ResponseBody ResponseEntity<String> uploadImage(
			@RequestParam(value = "image", required = true) MultipartFile file,
			@RequestParam(value = "name", required = false) String name) {

		String contentType = file.getContentType();
		byte[] imageData;
		try {
			imageData = file.getBytes();
		} catch (IOException e) {
			return new ResponseEntity<String>("Unable to read image data", HttpStatus.BAD_REQUEST);
		}
		if (!supportedImageTypes.contains(contentType)) {
			return ResponseEntity.badRequest().body("File format is not supported");
		}
		Image image = new Image(name != null ? name : file.getOriginalFilename(), imageData,
				contentType);
		UUID randomUUID = UUID.randomUUID();
		storage.put(randomUUID, image);

		return new ResponseEntity<String>("success", HttpStatus.OK);
	}

	@RequestMapping(value = "/{idAsString}", method = RequestMethod.GET)
	public @ResponseBody ResponseEntity<?> get(@PathVariable String idAsString) {
		UUID uuid;
		try {
			uuid = UUID.fromString(idAsString);
		} catch (IllegalArgumentException e) {
			logger.warn("Given id is not a legal UUID");
			return new ResponseEntity<String>("Given id is not a legal UUID", HttpStatus.BAD_REQUEST);
		}
		Optional<Image> result = storage.get(uuid);
		if (!result.isPresent()) {
			return new ResponseEntity<String>("image not found", HttpStatus.BAD_REQUEST);
		}

		HttpHeaders headers = new HttpHeaders();
		try {
			headers.setContentType(MediaType.parseMediaType(result.get().contentType));
		} catch (InvalidMediaTypeException e) {
			return new ResponseEntity<String>("stored media type is not an image mime-type",
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return new ResponseEntity<byte[]>(result.get().data, headers, HttpStatus.OK);
	}

	@RequestMapping(value = "/{idAsString}/thumb", method = RequestMethod.GET)
	public @ResponseBody ResponseEntity<byte[]> getImageThumbnail(@PathVariable String idAsString) throws IOException {
		UUID uuid;
		try {
			uuid = UUID.fromString(idAsString);
		} catch (IllegalArgumentException e) {
			logger.warn("Given id is not a legal UUID");
			return new ResponseEntity<byte[]>(HttpStatus.BAD_REQUEST);
		}
		Optional<Image> imageQuery = storage.get(uuid);
		if (!imageQuery.isPresent()) {
			return new ResponseEntity<byte[]>(HttpStatus.NOT_FOUND);
		}
		final Image image = imageQuery.get();
		ByteArrayOutputStream bos = null;
		try {
			BufferedImage buf = ImageIO.read(new ByteArrayInputStream(image.data));
			BufferedImage thumbnail = Scalr.resize(buf, 64, 64);
			bos = new ByteArrayOutputStream();
			ImageIO.write(thumbnail, "png", bos);
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.parseMediaType(image.contentType));
			headers.set("title", image.name);
			return new ResponseEntity<byte[]>(bos.toByteArray(), headers, HttpStatus.OK);
		} catch (IOException e) {
			return new ResponseEntity<byte[]>(HttpStatus.BAD_REQUEST);
		}
	}

	@RequestMapping(method = RequestMethod.GET)
	public @ResponseBody Set<UUID> getImageIds() {

		Set<UUID> list = storage.getKeys();
		Set<UUID> listUUIDs = new HashSet<>();
		for (UUID uuid : list) {
			listUUIDs.add(uuid);
		}

		return listUUIDs;
	}

	@RequestMapping(value = "/methods", method = RequestMethod.GET)
	public @ResponseBody List<String> methods() {
		return CommonlyUsedMethods.methodsWithRequestMapping(this.getClass());
	}

	@ExceptionHandler(MultipartException.class)
	public ModelAndView handleBadRequests(HttpServletRequest request, HttpServletResponse response,
			MultipartException ex) throws Exception {
		if (request.getRequestURI().equals("/images/new")) {
			response.setStatus(HttpStatus.BAD_REQUEST.value());
			response.setContentType("text/plain");
			ServletOutputStream outputStream = response.getOutputStream();
			DataOutputStream dos = new DataOutputStream(outputStream);
			dos.writeChars("File too big. Maximum 5MB");
			return null;
		}
		throw new Exception(ex);
	}

}
