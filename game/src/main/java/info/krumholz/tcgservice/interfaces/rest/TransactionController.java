package info.krumholz.tcgservice.interfaces.rest;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fasterxml.jackson.databind.ObjectMapper;

import info.krumholz.tcgservice.data.Transaction;
import info.krumholz.tcgservice.data.Transfer;
import info.krumholz.tcgservice.signing.Signed;
import info.krumholz.tcgservice.transactions.TransactionManager;

// TODO: implement
@Controller
@RequestMapping("/api/transactions")
public class TransactionController {

	@Autowired
	private TransactionManager transactionManager;

	@RequestMapping(method = RequestMethod.GET)
	public @ResponseBody Set<Signed<Transaction>> list() {
		return transactionManager.list();
	}

	@SuppressWarnings("unchecked")
	@RequestMapping(method = RequestMethod.POST)
	public @ResponseBody boolean submit(@RequestParam String transactions) {
		ObjectMapper mapper = new ObjectMapper();
		final List<Signed<Transaction>> readValue;
		if (transactions == null) {
			return false;
		} else {
			try {
				readValue = mapper.readValue(transactions, List.class);
			} catch (IOException e) {
				HttpHeaders header = new HttpHeaders();
				header.put("failureMessage", Arrays.asList(new String[] { e.getMessage() }));
				return false;
			}
		}
		return transactionManager.submitTransactions(readValue);
	}

	@RequestMapping(value = "/values", method = RequestMethod.GET)
	public @ResponseBody Set<UUID> getExistingValues() {
		Set<Signed<Transaction>> transactions = transactionManager.getCreationTransactions();
		Set<UUID> result = new HashSet<>();
		for (Signed<Transaction> transaction : transactions) {
			for (Transfer transfer : transaction.signable.transfers) {
				result.add(transfer.cardInstanceId);
			}
		}
		return result;
	}

}
