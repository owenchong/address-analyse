package com.ulife.addressanalyse.servlet;

import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;

/**
 * Servlet implementation class AnalyseServlet
 */
@WebServlet("/AnalyseServlet")
public class AnalyseServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private static List<Address> addresses = new ArrayList<Address>();
	private static ObjectMapper objectMapper = new ObjectMapper();

	static {
		InputStream inputStream = AnalyseServlet.class.getClassLoader().getResourceAsStream("address.json");
		try {
			JsonNode jsonNode = objectMapper.readTree(inputStream);
			Iterator<JsonNode> iterator = jsonNode.iterator();
			while (iterator.hasNext()) {
				JsonNode node = iterator.next();
				Address address = new Address();
				address.id = node.get("id").getLongValue();
				address.name = node.get("name").getTextValue();
				JsonNode points = node.get("points");
				Iterator<JsonNode> pointsIt = points.iterator();
				GeneralPath path = new GeneralPath();
				Point2D.Double fisrt = null;
				while (pointsIt.hasNext()) {
					JsonNode point = pointsIt.next();
					String textValue = point.getTextValue();
					String[] split = textValue.split("\\|");
					Point2D.Double p = new Point2D.Double();
					p.x = Double.valueOf(split[0]);
					p.y = Double.valueOf(split[1]);
					address.points.add(p);
					if (fisrt == null) {
						fisrt = p;
						path.moveTo(p.x, p.y);
					} else {
						path.lineTo(p.x, p.y);
					}
				}
				path.lineTo(fisrt.x,fisrt.y);
				address.path = path;
				addresses.add(address);
			}
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public AnalyseServlet() {
		super();
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException,
			IOException {
		doGet(request, response);
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		request.setCharacterEncoding("UTF-8");
		String address = request.getParameter("address");
		Point2D.Double addressPoint = getAddressPoint(address);
		Result result = new Result();
		if (addressPoint != null) {
			for (Address ad : addresses) {
				if (ad.path.contains(addressPoint)) {
					result.status = 0;
					result.addressId = ad.id;
					result.addressName = ad.name;
					break;
				}
			}
			if (result.status != 0) {
				result.errorMsg = "该地址没有站点";
			}
		} else {
			result.errorMsg = "地址没有找到坐标";
		}
		response.setContentType("text/plain;charset=utf-8");
		response.setCharacterEncoding("utf-8");
		PrintWriter writer = response.getWriter();
		writer.write(objectMapper.writeValueAsString(result));
		writer.close();
	}

	private Point2D.Double getAddressPoint(String address) {
		try {
			URL url = new URL("http://api.map.baidu.com/geocoder/v2/?address=" + URLEncoder.encode(address, "UTF-8")
					+ "&output=json&ak=jpxGGDcpZ89OELGV4WqQk0kr");
			JsonNode readTree = objectMapper.readTree(url.openStream());
			System.out.println(address + ":" + readTree);
			if (readTree.get("status").asInt() == 0) {
				JsonNode location = readTree.get("result").get("location");
				double lng = location.get("lng").asDouble();
				double lat = location.get("lat").asDouble();
				Point2D.Double point = new Point2D.Double(lng,lat);
				return point;
			} else {
				return null;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
/*
	private boolean isPtInPoly(double x, double y, List<Point> points) {
		int sum = 0, count;
		double x1, x2, y1, y2, dLon;
		if (points.size() < 3)
			return false;
		count = points.size();
		for (int i = 0; i < count - 1; i++) {
			if (i == count - 1) {
				x1 = points.get(i).x;
				y1 = points.get(i).y;
				x2 = points.get(0).x;
				y2 = points.get(0).y;
			} else {
				x1 = points.get(i).x;
				y1 = points.get(i).y;
				x2 = points.get(i + 1).x;
				y2 = points.get(i + 1).y;
			}
			// 以下语句判断A点是否在边的两端点的水平平行线之间，在则可能有交点，开始判断交点是否在左射线上
			if (((y >= y1) && (y < y2)) || ((y >= y2) && (y < y1))) {
				if (Math.abs(y1 - y2) > 0) {
					// 得到 A点向左射线与边的交点的x坐标：
					dLon = x1 - ((x1 - x2) * (y1 - y)) / (y1 - y2);
					// 如果交点在A点左侧（说明是做射线与 边的交点），则射线与边的全部交点数加一：
					if (dLon < x)
						sum++;
				}
			}
		}
		if (sum % 2 != 0)
			return true;
		return false;
	}*/
}

class Result {
	public int status = -1;// 0 success -1 failure
	public long addressId;
	public String addressName;
	public String errorMsg;
}

class Address {
	public Long id;
	public String name;
	public List<Point2D.Double> points = new ArrayList<Point2D.Double>();
	public GeneralPath path ;
}
/*
class Point {
	public double x;
	public double y;
}*/
