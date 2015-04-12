// CS 2XB3 Lab 2 - Final Project
// Hassaan Malik - 1224997
// Trevor Rae - 1324949
// Paul Warnick - 1300963

/*
 * Description:
 * 
 */

package navigation;

// imports for back end graph mapping
import graph.DijkstraSP;
import graph.Edge;
import graph.EdgeWeightedGraph;

// standard java imports
import java.awt.Color;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

// swing imports
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JOptionPane;
import javax.swing.JToolBar;

// arcGIS imports
import com.esri.toolkit.overlays.DrawingCompleteEvent;
import com.esri.toolkit.overlays.DrawingCompleteListener;
import com.esri.toolkit.overlays.DrawingOverlay;
import com.esri.toolkit.overlays.DrawingOverlay.DrawingMode;
import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.Polyline;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.PictureMarkerSymbol;
import com.esri.core.symbol.SimpleLineSymbol;
import com.esri.core.symbol.SimpleMarkerSymbol;
import com.esri.core.symbol.SimpleMarkerSymbol.Style;
import com.esri.core.tasks.na.NAFeaturesAsFeature;
import com.esri.map.GraphicsLayer;
import com.esri.map.JMap;
import com.esri.map.MapOptions;
import com.esri.map.MapOptions.MapType;

public class MapGenerator { // TODO sort variables, keep only needed fields (make the rest local)
	private JFrame window;
	private JMap map;
	private DrawingOverlay myDrawingOverlay; //Overlay to draw your route stops
	private GraphicsLayer stopsLayer;
	private GraphicsLayer streetsLayer;
	private GraphicsLayer routeLayer;
	private NAFeaturesAsFeature stops = new NAFeaturesAsFeature();
	private JButton startPointButton;
	private JButton stopButton;
	private JButton destinationButton;
	private JButton solveRouteButton;
  
	private int stopCounter = 0;
	private double[] latLongArrayStartPoint;
	private double[] latLongArrayEndPoint;
	private double[][] xyCoordinates;
	private static final String SOLVE_BUTTON = " Solve route "; // solve button string 
	private static final String RESET_BUTTON = " Reset "; // reset button string
	private static final String  STARTPOINT_BUTTON = " Choose Start Point ";
	private static final String  STOP_BUTTON = " Add a Stop ";
	private static final String  DESTINATION_BUTTON = " Choose Destination ";
	private static final String	STARTPOINT_IMAGE = "http://www.tactranconnect.com/images/icon_start.png"; // url for start image
	private static final String STOP_IMAGE = "http://www.tactranconnect.com/images/mapicons/marker_incidents.png"; // url for stop image
	private static final String	DESTINATION_IMAGE = "http://www.tactranconnect.com/images/icon_end.png"; // url for destination image
	private EdgeWeightedGraph graph;
	private static double[] esriCoordsArray;
	private static double[][] esriIntersectionCoordinates;
	private DijkstraSP shortestPathTree;
	private IntersectionsBST intersectionTree;
	private int[] stopArray = new int[1];
	private PictureMarkerSymbol startSymbol = new PictureMarkerSymbol(STARTPOINT_IMAGE); // creates a symbol with the start point url
	private PictureMarkerSymbol stopSymbol = new PictureMarkerSymbol(STOP_IMAGE); // creates a symbol with the start point url
	private PictureMarkerSymbol destinationSymbol = new PictureMarkerSymbol(DESTINATION_IMAGE); // same for destination
	private Edge[][] shortestPath;
	private boolean stopHasBeenClicked = false;
	private boolean destinationHasBeenClicked = false;
  
	// generates the map
	public MapGenerator() throws IOException {
		window = new JFrame();
		window.setSize(800, 600);
		window.setLocationRelativeTo(null); // center on screen
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		window.getContentPane().setLayout(new BorderLayout(0, 0));

		// dispose map just before application window is closed.
		window.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent windowEvent) {
				super.windowClosing(windowEvent);
				map.dispose();
			}
		});

		// Using MapOptions allows for a common online base map to be chosen
		MapOptions mapOptions = new MapOptions(MapType.GRAY_BASE);
		map = new JMap(mapOptions);
    
		// envelope for ocean
		map.setExtent(new Envelope(-16732452, 4533753, -16719762, 4619957.78));
		
		// adds graphic layers to the map
		routeLayer = new GraphicsLayer();
		map.getLayers().add(routeLayer);
		streetsLayer = new GraphicsLayer();
		map.getLayers().add(streetsLayer);
		stopsLayer = new GraphicsLayer();
		map.getLayers().add(stopsLayer);
		
		addStopGraphics();	
    
		// Add the JMap to the JFrame's content pane
		window.getContentPane().add(map);
    
		JLayeredPane contentPane = new JLayeredPane();
		contentPane.setLayout(new BorderLayout());
		contentPane.setVisible(true);
		window.add(contentPane);
		contentPane.add(map);
		contentPane.add(createToolBar(myDrawingOverlay), BorderLayout.NORTH);
		
		generateData(); 
	}
	
	// creates the BST for intersections, and graph from the input data files also draws the graph to the map 
	private void generateData() throws IOException {
		String currentLineString = "";
		BufferedReader inputIntersections = new BufferedReader(new FileReader("data/IntersectionsSJ.txt"));//reads the input file

		int lineCount = 0;

		while(inputIntersections.readLine()!=null){	lineCount++; }  //counts number of lines in the file

		graph = new EdgeWeightedGraph(lineCount);

		inputIntersections.close();

		inputIntersections = new BufferedReader(new FileReader("data/IntersectionsSJ.txt"));

		int coordinateCounter = 0;

		int[] streetID = new int[lineCount];
		esriIntersectionCoordinates = new double[lineCount][2];
		xyCoordinates = new double[lineCount][2];
		
		while(coordinateCounter != lineCount){
			currentLineString = inputIntersections.readLine();
			String[] currentLine = currentLineString.split(" ");
			streetID[coordinateCounter] = Integer.parseInt(currentLine[0]);
		
			// coords for ocean (Gray Scale)
			esriIntersectionCoordinates[coordinateCounter][0] = (-150.528512 + 0.000054142 * (Double.parseDouble(currentLine[1])));
			esriIntersectionCoordinates[coordinateCounter][1] = (38.247154 - 0.0000561075 * (Double.parseDouble(currentLine[2])));
		
			coordinateCounter++;
		}

		inputIntersections.close();

		coordinateCounter = 0;

		intersectionTree = new IntersectionsBST();

		while (coordinateCounter != lineCount){
			intersectionTree.insert(streetID[coordinateCounter], esriIntersectionCoordinates[coordinateCounter]); //inserts the values into a balanced BST
			coordinateCounter++;
		}

		currentLineString = "";

		BufferedReader inputStreets = new BufferedReader(new FileReader("data/StreetsSJ.txt"));//reads the input file

		lineCount = 0;

		while(inputStreets.readLine()!=null){ lineCount +=1; } //counts number of lines in the file

		inputStreets.close();

		inputStreets = new BufferedReader(new FileReader("data/StreetsSJ.txt"));

		coordinateCounter = 0;

		while(coordinateCounter != lineCount){
			currentLineString = inputStreets.readLine();
			String[] currentLine = currentLineString.split(" ");
			double[] xyStartCoordsArray = intersectionTree.search(Integer.parseInt(currentLine[1]));
			double[] xyEndCoordsArray = intersectionTree.search(Integer.parseInt(currentLine[2]));
		
			// gets the xy coordinate of the start point of a street
			double xCoordStartPoint = xyStartCoordsArray[0];
			double yCoordStartPoint = xyStartCoordsArray[1];
		
			// gets the xy coordinate of the end point of a street
			double xCoordEndPoint = xyEndCoordsArray[0];
			double yCoordEndPoint = xyEndCoordsArray[1];
		
			// creates the graph out of all the streets
			Edge edgeOneWay = new Edge(Integer.parseInt(currentLine[1]), Integer.parseInt(currentLine[2]), Double.parseDouble(currentLine[3]));
			graph.addEdge(edgeOneWay);
			Edge edgeOtherWay = new Edge(Integer.parseInt(currentLine[2]), Integer.parseInt(currentLine[1]), Double.parseDouble(currentLine[3]));
			graph.addEdge(edgeOtherWay);
		
			// adds a street graphic to the street layer of the map
			Polyline street = new Polyline();
			SimpleLineSymbol streetSymbol = new SimpleLineSymbol(Color.BLUE, 2.0f);
			latLongArrayStartPoint = convertToEsriMeters(xCoordStartPoint, yCoordStartPoint);
			street.startPath(latLongArrayStartPoint[0],latLongArrayStartPoint[1]);
			latLongArrayEndPoint = convertToEsriMeters(xCoordEndPoint, yCoordEndPoint);
			street.lineTo(latLongArrayEndPoint[0], latLongArrayEndPoint[1]);
			streetsLayer.addGraphic(new Graphic(street, streetSymbol, 0));
		
			coordinateCounter++;
		}

		inputStreets.close();
	}
  
	// adds the stop graphics to the map
	private void addStopGraphics() {
		myDrawingOverlay = new DrawingOverlay();
		myDrawingOverlay.addDrawingCompleteListener(new DrawingCompleteListener() {		
			@Override
			public void drawingCompleted(DrawingCompleteEvent event) {
				// get the user-drawn stop graphic from the overlay
				Graphic graphic = (Graphic) myDrawingOverlay.getAndClearFeature();
				// add it to the stopsLayer for display
				if (graphic.getSymbol().toString().contains("Size=0")) {}
			
				// adds the start point graphic stop layer upon mouse click
				else if (startPointButton.isEnabled()) {
					// features for adding stop graphic and enabling certain buttons
					startPointButton.setEnabled(false);
					stopsLayer.addGraphic(graphic);
					stopButton.setEnabled(true);
					destinationButton.setEnabled(true);
					stopCounter++;
				
					// finds the the closest intersection (vertex) on the map for navigation purposes
					String[] startPointLatInfo = (graphic.getGeometry().toString().split(","));
					String[] startPointLongInfo = startPointLatInfo[0].split("-");
					double startPointLatEsri = Double.parseDouble(startPointLongInfo[1]);
					startPointLatInfo[1] = startPointLatInfo[1].replace(' ', ']');
					startPointLatInfo[1] = startPointLatInfo[1].replace("]", "");
					double startPointLongEsri = Double.parseDouble(startPointLatInfo[1]);
				
					// checks if the start point location is out of bounds
					if ((getClosestIntersection(startPointLatEsri, startPointLongEsri)[0]) == -1.0 || getClosestIntersection(startPointLatEsri, startPointLongEsri)[1] == -1.0) {
						startPointButton.setEnabled(true);
						stopButton.setEnabled(false);
						destinationButton.setEnabled(false);
						solveRouteButton.setEnabled(false);
						myDrawingOverlay.setEnabled(true);
						stopArray = new int[1];
						stopHasBeenClicked = false;
						destinationHasBeenClicked = false;
						
						// reset graphic layers, stop features and global variables
						stopCounter = 0;
						routeLayer.removeAll();
						stopsLayer.removeAll();
						stops.clearFeatures();
						HashMap<String, Object> attributes = new HashMap<String, Object>();
						attributes.put("type", "Start");
						myDrawingOverlay.setUp(
								DrawingMode.POINT,
								new SimpleMarkerSymbol(Color.GRAY, 0, Style.X),
								attributes);
					}
					
					else {
						double startClosestNodeLat = getClosestIntersection(startPointLatEsri, startPointLongEsri)[0];
						double startClosestNodeLong = getClosestIntersection(startPointLatEsri, startPointLongEsri)[1];
						intersectionTree.findNodeID(intersectionTree.root, startClosestNodeLat, startClosestNodeLong);
						stopArray[0] = intersectionTree.closestNode;
					}
				}
				
				// adds the destination graphic to the stop layer upon mouse click
				else if (!destinationButton.isEnabled() && stopCounter == 1) {
					// modifies which buttons are enabled and adds the destination graphic
					stopsLayer.addGraphic(graphic);
					solveRouteButton.setEnabled(true);
					stopCounter++;
					
					// finds the closest intersection (vertex) on the map for navigation purposes
					String[] destinationLatInfo = (graphic.getGeometry().toString().split(","));
					String[] destinationLongInfo = destinationLatInfo[0].split("-");
					double destinationLatEsri = Double.parseDouble(destinationLongInfo[1]);
					destinationLatInfo[1] =  destinationLatInfo[1].replace(' ', ']');
					destinationLatInfo[1] =  destinationLatInfo[1].replace("]", "");
					double destinationLongEsri = Double.parseDouble( destinationLatInfo[1]);
				
					// checks if the destination location is out of bounds
					if ((getClosestIntersection(destinationLatEsri, destinationLongEsri)[0]) == -1.0 || getClosestIntersection(destinationLatEsri, destinationLongEsri)[1] == -1.0) {
						startPointButton.setEnabled(true);
						destinationButton.setEnabled(false);
						solveRouteButton.setEnabled(false);
						myDrawingOverlay.setEnabled(true);
						stopArray = new int[1];
						stopHasBeenClicked = false;
						destinationHasBeenClicked = false;
						
						// reset graphic layers, stop features and global variables
						stopCounter = 0;
						routeLayer.removeAll();
						stopsLayer.removeAll();
						stops.clearFeatures();
						HashMap<String, Object> attributes = new HashMap<String, Object>();
						attributes.put("type", "Start");
						myDrawingOverlay.setUp(
								DrawingMode.POINT,
								new SimpleMarkerSymbol(Color.GRAY, 0, Style.X),
								attributes);
					}
				
					else {
						double destinationClosestNodeLat = getClosestIntersection(destinationLatEsri, destinationLongEsri)[0];
						double destinationClosestNodeLong = getClosestIntersection(destinationLatEsri, destinationLongEsri)[1];
						intersectionTree.findNodeID(intersectionTree.root, destinationClosestNodeLat, destinationClosestNodeLong);
						int[] closestNode = {intersectionTree.closestNode};
						addToStops(stopArray, closestNode);
					}
				}
				
				else if (stopButton.isEnabled() && stopHasBeenClicked && !destinationHasBeenClicked) { 
					// modifies which buttons are enabled and adds the stop graphic
					stopsLayer.addGraphic(graphic);
					
					// finds the closest intersection (vertex) on the map for navigation purposes
					String[] stopLatInfo = (graphic.getGeometry().toString().split(","));
					String[] stopLongInfo = stopLatInfo[0].split("-");
					double stopLatEsri = Double.parseDouble(stopLongInfo[1]);
					stopLatInfo[1] =  stopLatInfo[1].replace(' ', ']');
					stopLatInfo[1] =  stopLatInfo[1].replace("]", "");
					double stopLongEsri = Double.parseDouble( stopLatInfo[1]);
				
					// checks if the stop location is out of bounds
					if ((getClosestIntersection(stopLatEsri, stopLongEsri)[0]) == -1.0 || getClosestIntersection(stopLatEsri, stopLongEsri)[1] == -1.0) {
						startPointButton.setEnabled(true);
						stopButton.setEnabled(false);
						solveRouteButton.setEnabled(false);
						myDrawingOverlay.setEnabled(true);
						stopArray = new int[1];
						stopHasBeenClicked = false;
						destinationHasBeenClicked = false;
						
						// reset graphic layers, stop features and global variables
						stopCounter = 0;
						routeLayer.removeAll();
						stopsLayer.removeAll();
						stops.clearFeatures();
						HashMap<String, Object> attributes = new HashMap<String, Object>();
						attributes.put("type", "Start");
						myDrawingOverlay.setUp(
								DrawingMode.POINT,
								new SimpleMarkerSymbol(Color.GRAY, 0, Style.X),
								attributes);
					}
				
					else {
						double stopClosestNodeLat = getClosestIntersection(stopLatEsri, stopLongEsri)[0];
						double stopClosestNodeLong = getClosestIntersection(stopLatEsri, stopLongEsri)[1];
						intersectionTree.findNodeID(intersectionTree.root, stopClosestNodeLat, stopClosestNodeLong);
						int[] closestNode = {intersectionTree.closestNode};
						addToStops(stopArray, closestNode);
					}
				}
			}
		});
		map.addMapOverlay(myDrawingOverlay);
	}
	
	public void addToStops(int[] currentStopArray, int[] toBeAdded) {
		   int currentStopArrayLength = currentStopArray.length;
		   int toBeAddedLenth = toBeAdded.length;
		   stopArray= new int[currentStopArrayLength + toBeAddedLenth];
		   System.arraycopy(currentStopArray, 0, stopArray, 0, currentStopArrayLength);
		   System.arraycopy(toBeAdded, 0, stopArray, currentStopArrayLength, toBeAddedLenth);
		}

	// adds the optimal route to the map 
	private void displayRoute(Edge[] route, boolean isLoop) {
		if (isLoop == true) {
			if(route[0]!=null){
				Polyline routeStreet = new Polyline();
				SimpleLineSymbol routeSymbol = new SimpleLineSymbol(Color.GREEN, 6.0f);
				double[] latLongArrayStartPointRoute = convertToEsriMeters((intersectionTree.search(route[0].either())[0]), (intersectionTree.search(route[0].either())[1])); 
				double[] latLongArrayEndPointRoute; 
				routeStreet.startPath(latLongArrayStartPointRoute[0], latLongArrayStartPointRoute[1]);
				
				for (int i = 0; i < route.length; i++) {
					if(route[i]!=null){
						latLongArrayEndPointRoute = convertToEsriMeters((intersectionTree.search(route[i].other(route[i].either()))[0]), (intersectionTree.search(route[i].other(route[i].either()))[1]));
						routeStreet.lineTo(latLongArrayEndPointRoute[0], latLongArrayEndPointRoute[1]);
					}
				}
				routeLayer.addGraphic(new Graphic(routeStreet, routeSymbol, 0));
			}
		}
		
		else {
			Polyline routeStreet = new Polyline();
			SimpleLineSymbol routeSymbol = new SimpleLineSymbol(Color.RED, 10.0f);
			double[] latLongArrayStartPointRoute = convertToEsriMeters((intersectionTree.search(route[0].either())[0]), (intersectionTree.search(route[0].either())[1])); 
			double[] latLongArrayEndPointRoute; 
			routeStreet.startPath(latLongArrayStartPointRoute[0], latLongArrayStartPointRoute[1]);
			
			for (int i = 0; i < route.length; i++) {
				latLongArrayEndPointRoute = convertToEsriMeters((intersectionTree.search(route[i].other(route[i].either()))[0]), (intersectionTree.search(route[i].other(route[i].either()))[1]));
				routeStreet.lineTo(latLongArrayEndPointRoute[0], latLongArrayEndPointRoute[1]);
			}
			routeLayer.addGraphic(new Graphic(routeStreet, routeSymbol, 0));
		}
	}
  
	// determines the closest intersection to where the user has clicked
	private double[] getClosestIntersection(double esriLat, double esriLong) {
		double[] closestIntersection = new double[2];
		double distance = 0;
		double tempDistance = Double.POSITIVE_INFINITY;
		double [] esriCoords = { -esriLat, esriLong };
		esriCoords[0] = convertFromEsriMeters(esriCoords)[0];
		esriCoords[1] = convertFromEsriMeters(esriCoords)[1];
		
		
		for (int i = 0; i < xyCoordinates.length; i++) {
			distance = (Math.sqrt((Math.pow((esriCoords[0] - (esriIntersectionCoordinates[i][0])),2)) + (Math.pow(esriCoords[1] - (esriIntersectionCoordinates[i][1]),2))));
			if (distance < tempDistance) {
				tempDistance = distance;
				closestIntersection[0] = esriIntersectionCoordinates[i][0];
				closestIntersection[1] = esriIntersectionCoordinates[i][1];
			}
		}
		
		if (tempDistance > 0.07) {
		 JOptionPane.showMessageDialog(window,
				 "One of the locations you've selected is to far away from the map! Please pick another",
				 "Warning",
				 JOptionPane.WARNING_MESSAGE);				 
		 double[] error = { -1.0, -1.0 };
		 return (error);
		}
	  
		return closestIntersection;
	}
  
	// uses Dijkstra's algorithm to find the optimal route in the graph from user chose start, to destination
	@SuppressWarnings("unused")
	private void getRoute(EdgeWeightedGraph g, int[] stopArray){
		shortestPath = new Edge[stopArray.length - 1][];
		for (int i = 0; i < stopArray.length - 1; i++) {
			int pathLengthCounter = 0;
			Edge[] edgeArray = {};
			shortestPathTree = new DijkstraSP(g, stopArray[i]);
			
			if (shortestPathTree.hasPathTo(stopArray[i + 1])) {				
				for (Edge currentEdge : shortestPathTree.pathTo(stopArray[i + 1])) {
					pathLengthCounter++;	      		
				}
				
				if (pathLengthCounter != 0) {				
				   	edgeArray = new Edge[pathLengthCounter];
		        	int setCurrentEdgeCounter = 0;
		        	
		        	for (Edge currentEdge : shortestPathTree.pathTo(stopArray[i + 1])) {
		            	edgeArray[setCurrentEdgeCounter] = currentEdge;
		            	setCurrentEdgeCounter++;
		        	}
		        	
		        	Collections.reverse(Arrays.asList(edgeArray));
		        	
		        	pathLengthCounter = 0;
				  	displayRoute(edgeArray, false);
				  	shortestPath[i] = new Edge[edgeArray.length];
				  	for (int j = 0; j < edgeArray.length; j++) {
				  		shortestPath[i][j] = edgeArray[j];
				  	}
				}
			}			  	
		}
	}
	
	@SuppressWarnings("unused")
	private Edge[] rightTurnLoop(Iterable<Edge> intersectionAdjList, Edge firstEdge){
		int listChecker = 1;
		int firstEdgeV = firstEdge.either();
		int firstEdgeW = firstEdge.other(firstEdge.either());
		double[] firstIntersection = intersectionTree.search(firstEdgeV);
		double firstIntersectionX = ((firstIntersection[0] + 150.528512) / 0.000054142);
		double firstIntersectionY = ((firstIntersection[1] - 38.247154) / (-0.0000561075));
		double[] middleIntersection = intersectionTree.search(firstEdgeW);
		double middleIntersectionX = ((middleIntersection[0] + 150.528512) / 0.000054142);
		double middleIntersectionY = ((middleIntersection[1] - 38.247154) / (-0.0000561075));
		double firstEdgeAngle = -Math.toDegrees(Math.atan2((middleIntersectionY - firstIntersectionY), (middleIntersectionX - firstIntersectionX)));
		Edge[] edges = new Edge[4];
		if (firstEdgeAngle < 0) {
			firstEdgeAngle += 360;
		}
		for (Edge adjEdge : intersectionAdjList) {
			if(firstEdgeW == adjEdge.either()){
				int tempEdgeW = adjEdge.getW();
				double[] tempIntersection = intersectionTree.search(tempEdgeW);
				double tempIntersectionX = ((tempIntersection[0] + 150.528512) / 0.000054142);
				double tempIntersectionY = ((tempIntersection[1] - 38.247154) / (-0.0000561075));
				double secondEdgeAngle = -Math.toDegrees(Math.atan2((tempIntersectionY - middleIntersectionY), (tempIntersectionX - middleIntersectionX)));
				
				if (secondEdgeAngle < 0 ) {
					secondEdgeAngle += 360;
				}
				if(Math.abs((secondEdgeAngle-firstEdgeAngle))<=20){
					edges[0] = adjEdge;
					edges[1] = rightTurnChecker(adjEdge);
					edges[2] = rightTurnChecker(edges[1]);
					edges[3] = rightTurnChecker(edges[2]);
					if(edges[3]== null || edges[3].getW()!=firstEdge.getW()){
						edges[0]=null;
						edges[1]=null;
						edges[2]=null;
						edges[3]=null;
					}
				}
			}
			listChecker++;
		}
		return edges;
	}
	
	private Edge rightTurnChecker(Edge firstEdge){
		if(firstEdge!=null){
			int firstEdgeV = firstEdge.either();
			int firstEdgeW = firstEdge.other(firstEdge.either());
			
			double[] firstIntersection = intersectionTree.search(firstEdgeV);
			double[] middleIntersection = intersectionTree.search(firstEdgeW);
			
			double firstIntersectionX = ((firstIntersection[0] + 150.528512) / 0.000054142);
			double firstIntersectionY = ((firstIntersection[1] - 38.247154) / (-0.0000561075));
			
			double middleIntersectionX = ((middleIntersection[0] + 150.528512) / 0.000054142);
			double middleIntersectionY = ((middleIntersection[1] - 38.247154) / (-0.0000561075));
			
			Iterable<Edge> intersectionAdjList = graph.adj(firstEdgeW);
			int checkEitherCounter = 1;
			int intersectionID = -1;
			double[] tempAdjIntersection;
			double tempAdjAngle; 
			
			double firstEdgeAngle = -Math.toDegrees(Math.atan2((middleIntersectionY - firstIntersectionY), (middleIntersectionX - firstIntersectionX)));
			
			if (firstEdgeAngle < 0) {
				firstEdgeAngle += 360;
			}
			
			for (Edge adjEdge : intersectionAdjList) {
				if (checkEitherCounter == 2) {
					intersectionID = adjEdge.either();
				}
				
				if (intersectionID == adjEdge.either()) {
					
					tempAdjIntersection = intersectionTree.search(adjEdge.other(adjEdge.either()));
					double tempAdjIntersectionX = ((tempAdjIntersection[0] + 150.528512) / 0.000054142);
					double tempAdjIntersectionY = ((tempAdjIntersection[1] - 38.247154) / (-0.0000561075));
					tempAdjAngle = -Math.toDegrees(Math.atan2((tempAdjIntersectionY - middleIntersectionY), (tempAdjIntersectionX - middleIntersectionX)));
					
					if (tempAdjAngle < 0) {
						tempAdjAngle += 360;
					}
					
					if (firstEdgeAngle >= 0 && firstEdgeAngle < 180) { // first & second quad
						if ((firstEdgeAngle < 10) && (tempAdjAngle > (firstEdgeAngle + 190)) && (tempAdjAngle <= 350)){
							return(adjEdge);
						}
						
						else if ((tempAdjAngle > (firstEdgeAngle + 180)) && (tempAdjAngle <= 360)) { // if left
							return(adjEdge);
						}
						
						else if ((tempAdjAngle < (firstEdgeAngle - 10)) && (tempAdjAngle >= 0)) {
							return(adjEdge);
						}
					}	
					
					else { // third & forth quad
						if ((tempAdjAngle < (firstEdgeAngle - 10)) && (tempAdjAngle > (firstEdgeAngle - 170))) {
							return(adjEdge);
						}
					}
				}
				checkEitherCounter++;
			}
		}		
		return(null);
	}
  
	
	private void leftTurnChecker() {
		for (int j = 0; j < shortestPath.length; j++) {
			if (shortestPath[j] != null) {
				for (int i = 1; i < shortestPath[j].length; i++) {
					Edge firstEdge = shortestPath[j][i - 1];
					Edge secondEdge = shortestPath[j][i];
					int firstEdgeV = firstEdge.either();
					int firstEdgeW = firstEdge.other(firstEdge.either());
					int secondEdgeV = secondEdge.other(secondEdge.either());
					
					double[] firstIntersection = intersectionTree.search(firstEdgeV);
					double[] middleIntersection = intersectionTree.search(firstEdgeW);
					double[] lastIntersection = intersectionTree.search(secondEdgeV);
					
					double firstIntersectionX = ((firstIntersection[0] + 150.528512) / 0.000054142);
					double firstIntersectionY = ((firstIntersection[1] - 38.247154) / (-0.0000561075));
					
					double middleIntersectionX = ((middleIntersection[0] + 150.528512) / 0.000054142);
					double middleIntersectionY = ((middleIntersection[1] - 38.247154) / (-0.0000561075));
					
					double lastIntersectionX = ((lastIntersection[0] + 150.528512) / 0.000054142);
					double lastIntersectionY = ((lastIntersection[1] - 38.247154) / (-0.0000561075));
					
					double firstEdgeAngle = -Math.toDegrees(Math.atan2((middleIntersectionY - firstIntersectionY), (middleIntersectionX - firstIntersectionX)));
					double secondEdgeAngle = -Math.toDegrees(Math.atan2((lastIntersectionY - middleIntersectionY), (lastIntersectionX - middleIntersectionX)));
						
					if (firstEdgeAngle < 0) {
						firstEdgeAngle += 360;
					}
					
					if (secondEdgeAngle < 0) {
						secondEdgeAngle += 360;
					}
					
					if (firstEdgeAngle >= 0 && firstEdgeAngle < 180) { // first & second quad
						if ((secondEdgeAngle > firstEdgeAngle) && (secondEdgeAngle < (firstEdgeAngle + 180))) { // if left
							Iterable<Edge> intersectionAdjList = graph.adj(firstEdgeW);
							int checkEitherCounter = 1;
							int intersectionID = -1;
							double[] tempAdjIntersection;
							double tempAdjAngle; 
							
							for (Edge adjEdge : intersectionAdjList) {
								if (checkEitherCounter == 2) {
									intersectionID = adjEdge.either();
								}
								
								if (intersectionID == adjEdge.either()) {
									
									tempAdjIntersection = intersectionTree.search(adjEdge.other(adjEdge.either()));
									double tempAdjIntersectionX = ((tempAdjIntersection[0] + 150.528512) / 0.000054142);
									double tempAdjIntersectionY = ((tempAdjIntersection[1] - 38.247154) / (-0.0000561075));
									tempAdjAngle = -Math.toDegrees(Math.atan2((tempAdjIntersectionY - middleIntersectionY), (tempAdjIntersectionX - middleIntersectionX)));
									
									if (tempAdjAngle < 0) {
										tempAdjAngle += 360;
									}
									
									if ((tempAdjAngle < secondEdgeAngle) && (tempAdjAngle > (firstEdgeAngle - 20))) {
										//System.out.println("Left 1/2");
										Edge[] threeRightTurns = rightTurnLoop(intersectionAdjList,firstEdge);
										displayRoute(threeRightTurns, true);
									}							
								}
								checkEitherCounter++;
							}
						}
					}
					
					else { // third & forth quad
						if ((secondEdgeAngle > firstEdgeAngle) || ((secondEdgeAngle > 0) && (secondEdgeAngle < (firstEdgeAngle - 180)))) { // if left
							if (secondEdgeAngle > firstEdgeAngle) { // if second edge angle is between first and 360
								Iterable<Edge> intersectionAdjList = graph.adj(firstEdgeW);
								int checkEitherCounter = 1;
								int intersectionID = -1;
								double[] tempAdjIntersection;
								double tempAdjAngle; 
								
								for (Edge adjEdge : intersectionAdjList) {
									if (checkEitherCounter == 2) {
										intersectionID = adjEdge.either();
									}
									
									if (intersectionID == adjEdge.either()) {
										
										tempAdjIntersection = intersectionTree.search(adjEdge.other(adjEdge.either()));
										double tempAdjIntersectionX = ((tempAdjIntersection[0] + 150.528512) / 0.000054142);
										double tempAdjIntersectionY = ((tempAdjIntersection[1] - 38.247154) / (-0.0000561075));
										tempAdjAngle = -Math.toDegrees(Math.atan2((tempAdjIntersectionY - middleIntersectionY), (tempAdjIntersectionX - middleIntersectionX)));
										
										if (tempAdjAngle < 0) {
											tempAdjAngle += 360;
										}
										
										if ((tempAdjAngle < secondEdgeAngle) && (tempAdjAngle > (firstEdgeAngle - 20))) {
											Edge[] threeRightTurns = rightTurnLoop(intersectionAdjList,firstEdge);
											displayRoute(threeRightTurns, true);
										}							
									}
									checkEitherCounter++;
								}
							}
							
							else if (secondEdgeAngle < firstEdgeAngle) { // if second edge angle is between first and 0
								Iterable<Edge> intersectionAdjList = graph.adj(firstEdgeW);
								int checkEitherCounter = 1;
								int intersectionID = -1;
								double[] tempAdjIntersection;
								double tempAdjAngle; 
								
								for (Edge adjEdge : intersectionAdjList) {
									if (checkEitherCounter == 2) {
										intersectionID = adjEdge.either();
									}
									
									if (intersectionID == adjEdge.either()) {
										
										tempAdjIntersection = intersectionTree.search(adjEdge.other(adjEdge.either()));
										double tempAdjIntersectionX = ((tempAdjIntersection[0] + 150.528512) / 0.000054142);
										double tempAdjIntersectionY = ((tempAdjIntersection[1] - 38.247154) / (-0.0000561075));
										tempAdjAngle = -Math.toDegrees(Math.atan2((tempAdjIntersectionY - middleIntersectionY), (tempAdjIntersectionX - middleIntersectionX)));
										
										if (tempAdjAngle < 0) {
											tempAdjAngle += 360;
										}
										
										if ((tempAdjAngle >= 0) && (tempAdjAngle <= secondEdgeAngle)) { 
											Edge[] threeRightTurns = rightTurnLoop(intersectionAdjList,firstEdge);
											displayRoute(threeRightTurns, true);
										}
									}
									checkEitherCounter++;
								}
							}
						}
					}
				}	
			}
		}
	}
	
	// converts an input latitude longitude to ESRI meters to display on the map properly
	private double[] convertToEsriMeters(double longitude, double latitude) {
		if ((Math.abs(longitude) > 180 || Math.abs(latitude) > 90)) { return null; }
		
		double num = longitude * 0.017453292519943295;
	    double x = 6378137.0 * num;
	    double a = latitude * 0.017453292519943295;
	
	    longitude = x;
	    latitude = 3189068.5 * Math.log((1.0 + Math.sin(a)) / (1.0 - Math.sin(a)));
	    esriCoordsArray = new double[2];
	    esriCoordsArray[0] = longitude;
	    esriCoordsArray[1] = latitude;
	    return esriCoordsArray;
	}
  
	// converts from esri meters to regular latitude
	private static double[] convertFromEsriMeters(double[] esri) {
		double x = 0;
		double y = 0;
		x = esri[0];
		y = esri[1];
		
		double num1 = Math.pow(Math.E, y/3189068.5);
		double latitude = Math.asin((num1 -1)/(1+num1));
		
		latitude = latitude/0.017453292519943295;
		double longitude = x/6378137.0;
		longitude = longitude/0.017453292519943295;
		double[] ret = new double[2];
		ret[0]= longitude;
		ret[1]= latitude;
		return ret;
	}
  
	// creates the tool bar containing the buttons
	private Component createToolBar(DrawingOverlay drawingOverlay) {
		JToolBar toolBar = new JToolBar();
		toolBar.setLayout(new FlowLayout(FlowLayout.CENTER));
		toolBar.setFloatable(false);
		
		// add Start Point button
		startPointButton = new JButton(STARTPOINT_BUTTON);
		startPointButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				startSymbol.setOffsetY((float) 16); // shifts the picture upwards
				HashMap<String, Object> attributes = new HashMap<String, Object>();
			    attributes.put("type", "Start");
			    drawingOverlay.setUp(
			        DrawingMode.POINT,
			        startSymbol, // the picutre for the stop
			        attributes);
			}
		});
		toolBar.add(startPointButton);
		
		// add Stop button
		stopButton = new JButton(STOP_BUTTON);
		stopButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				stopHasBeenClicked = true;
				stopSymbol.setOffsetY((float) 16); // shifts the picture upwards
				HashMap<String, Object> attributes = new HashMap<String, Object>();
			    attributes.put("type", "Stop");
			    drawingOverlay.setUp(
			        DrawingMode.POINT,
			        stopSymbol, // the picutre for the stop
			        attributes);
			}
		});
		toolBar.add(stopButton);
		stopButton.setEnabled(false);
		
		// add Destination button
		destinationButton = new JButton(DESTINATION_BUTTON);
		destinationButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				destinationHasBeenClicked = true;
				destinationSymbol.setOffsetY((float) 16); // shifts the picture upwards
				HashMap<String, Object> attributes = new HashMap<String, Object>();
			    attributes.put("type", "Destination");
			    drawingOverlay.setUp(
			        DrawingMode.POINT,
			        destinationSymbol, // the picture for the stop
			        attributes);
			    stopButton.setEnabled(false);
			    destinationButton.setEnabled(false);
			}
		});
		toolBar.add(destinationButton);
		destinationButton.setEnabled(false);

		// solve route button
		solveRouteButton = new JButton(SOLVE_BUTTON);
		solveRouteButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// disable the toolbar buttons and overlay 
				startPointButton.setEnabled(false);
				stopButton.setEnabled(false);
			    destinationButton.setEnabled(false);
			    solveRouteButton.setEnabled(false);
			    myDrawingOverlay.setEnabled(false);
			    getRoute(graph, stopArray);
			    leftTurnChecker();
			}
		});
		toolBar.add(solveRouteButton);
		solveRouteButton.setEnabled(false);

		// reset button
		JButton resetButton = new JButton(RESET_BUTTON);
		resetButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// enable the toolbar buttons and overlay  
			    startPointButton.setEnabled(true);
			    stopButton.setEnabled(false);
			    destinationButton.setEnabled(false);
			    solveRouteButton.setEnabled(false);
			    myDrawingOverlay.setEnabled(true);
			    stopArray = new int[1];
			    stopHasBeenClicked = false;
				destinationHasBeenClicked = false;
			    
			    // reset graphic layers, stop features and global variables
			    stopCounter = 0;
			    routeLayer.removeAll();
			    stopsLayer.removeAll();
			    stops.clearFeatures();
			    HashMap<String, Object> attributes = new HashMap<String, Object>();
			    attributes.put("type", "Start");
			    drawingOverlay.setUp(
			    		DrawingMode.POINT,
			    		new SimpleMarkerSymbol(Color.GRAY, 0, Style.X),
			    		attributes);
			}
		});
		toolBar.add(resetButton);
		return toolBar;
	}

	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					MapGenerator application = new MapGenerator();
					application.window.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
}