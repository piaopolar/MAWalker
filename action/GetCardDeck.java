package action;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.w3c.dom.Document;

import walker.ErrorData;
import walker.Info;
import walker.Process;

public class GetCardDeck {
	public static final ActionRegistry.Action Name = ActionRegistry.Action.GET_CARD_DECK;

	private static final String URL_GET_CARD_DECK = "http://web.million-arthurs.com/connect/app/roundtable/edit?cyt=1";
	private static byte[] response;

	public static boolean run() throws Exception {
		Document doc;
		ArrayList<NameValuePair> post = new ArrayList<NameValuePair>();
		post.add(new BasicNameValuePair("move", "1"));
		try {
			response = walker.Process.network.ConnectToServer(
					URL_GET_CARD_DECK, post, false);
		} catch (Exception ex) {
			ErrorData.currentDataType = ErrorData.DataType.text;
			ErrorData.currentErrorType = ErrorData.ErrorType.ConnectionError;
			ErrorData.text = ex.getMessage();
			throw ex;
		}

		Thread.sleep(Process.getRandom(1000, 2000));

		if (Info.Debug) {
			File outputFile = new File("GET_CARD_DECK.xml");
			FileOutputStream outputFileStream = new FileOutputStream(outputFile);
			outputFileStream.write(response);
			outputFileStream.close();
		}

		try {
			doc = Process.ParseXMLBytes(response);
		} catch (Exception ex) {
			ErrorData.currentDataType = ErrorData.DataType.bytes;
			ErrorData.currentErrorType = ErrorData.ErrorType.GetCardDeckDataError;
			ErrorData.bytes = response;
			throw ex;
		}

		try {
			return parse(doc);
		} catch (Exception ex) {
			throw ex;
		}
	}

	private static boolean parse(Document doc) throws Exception {

		XPathFactory factory = XPathFactory.newInstance();
		XPath xpath = factory.newXPath();

		try {
			if (!xpath.evaluate("/response/header/error/code", doc).equals("0")) {
				ErrorData.currentErrorType = ErrorData.ErrorType.GetCardDeckResponse;
				ErrorData.currentDataType = ErrorData.DataType.text;
				ErrorData.text = xpath.evaluate(
						"/response/header/error/message", doc);
				return false;
			}

			Info.MyDeck0.card = xpath.evaluate(
					"//deck[deckname='デッキ1']/deck_cards", doc);
			Info.MyDeck1.card = xpath.evaluate(
					"//deck[deckname='デッキ2']/deck_cards", doc);
			Info.MyDeck2.card = xpath.evaluate(
					"//deck[deckname='デッキ3']/deck_cards", doc);

			// leader不用获取
			Info.MyDeck0.leader = xpath.evaluate(
					"//deck[deckname='デッキ1']/leader_card", doc);
			Info.MyDeck1.leader = xpath.evaluate(
					"//deck[deckname='デッキ2']/leader_card", doc);
			Info.MyDeck2.leader = xpath.evaluate(
					"//deck[deckname='デッキ3']/leader_card", doc);

			walker.Go.saveDeck(0, Info.MyDeck0.card);
			walker.Go.saveDeck(1, Info.MyDeck1.card);
			walker.Go.saveDeck(2, Info.MyDeck2.card);

		} catch (Exception ex) {
			if (ErrorData.currentErrorType != ErrorData.ErrorType.none)
				throw ex;
			ErrorData.currentDataType = ErrorData.DataType.bytes;
			ErrorData.currentErrorType = ErrorData.ErrorType.GetCardDeckDataParseError;
			ErrorData.bytes = response;
			throw ex;
		}

		return true;
	}
}