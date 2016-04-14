package info.krumholz.tcgservice.utils;

import java.util.Objects;
import java.util.Optional;

public class Result<T> {

	private final T value;
	private final String failureReason;

	private Result(final T result, final String failureMessage) {
		this.value = result;
		this.failureReason = failureMessage;
	}

	public static final <T> Result<T> success(final T result) {
		return new Result<T>(result, null);
	}

	public static final <T> Result<T> success(final Optional<T> result) {
		if (result.isPresent()) {
			return success(result.get());
		}
		return failure("Given Optional was empty");
	}

	public static final <T> Result<T> failure(String reason) {
		return new Result<T>(null, reason);
	}

	public static final <T> Result<T> failure(String formatString, Object... args) {
		return new Result<T>(null, String.format(formatString, args));
	}

	public boolean isFailed() {
		return failureReason != null;
	}

	public String getFailureReason() {
		return failureReason;
	}

	public T get() {
		if (isFailed())
			throw new CantGetFailedResult();
		return value;
	}

	@Override
	public String toString() {
		return failureReason != null ? String.format("Success[%s]", value)
				: String.format("Failure[%s]", failureReason);
	}

	@Override
	public int hashCode() {
		if (isFailed()) {
			return Objects.hash(failureReason);
		}
		return Objects.hash(value);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (!(obj instanceof Result)) {
			return false;
		}

		Result<?> other = (Result<?>) obj;
		if (isFailed() && other.isFailed()) {
			return failureReason.equals(other.failureReason);
		}

		if (!isFailed() && !other.isFailed()) {
			return Objects.equals(value, other.value);
		}

		return false;
	}
}
