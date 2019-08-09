package monic;

import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.time.LocalTime;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * MONitor Internet Connection. This class monitors reachability of a given
 * host, and displays a connection status icon in the task bar.
 *
 * @author lbroquet
 */
public class Main {

	private static final ResourceBundle bundle = ResourceBundle.getBundle("monic.config");
	private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
	private static final Timer timer = new Timer("Periodic checker");
	private static final Image red;
	private static final Image green;
	private static final PopupMenu menu = new PopupMenu();
	private static final ActionListener exitListener = evt -> {
		timer.cancel();
		try {
			setIcon(null, null);
		} catch (AWTException ex) {
			// Should never happen. However, should not be a problem anyway.
			LOGGER.log(Level.WARNING, null, ex);
		}
	};

	static {
		URL redUrl = ClassLoader.getSystemResource(bundle.getString("icon.red"));
		URL greenUrl = ClassLoader.getSystemResource(bundle.getString("icon.green"));
		red = Toolkit.getDefaultToolkit().getImage(redUrl);
		green = Toolkit.getDefaultToolkit().getImage(greenUrl);

		MenuItem exitItem = new MenuItem(bundle.getString("menu.item.exit.label"));
		exitItem.addActionListener(exitListener);
		menu.add(exitItem);
	}

	private Main() {
		// utility class
	}

	/**
	 * Sets the application's tray icon. The icon will be associated with the
	 * given tooltip and a pop-up menu to interrupt the application.
	 *
	 * @param image the image to display or {@code null} to remove the icon
	 * @param tooltip the tooltip that will appear when the mouse hovers the
	 * icon
	 * @throws AWTException if an exception occurs when replacing the icon.<br>
	 * This exception may be raise only if {@code image} is not {@code null}.
	 */
	private static void setIcon(Image image, String tooltip) throws AWTException {
		SystemTray systemTray = SystemTray.getSystemTray();
		for (TrayIcon icon : systemTray.getTrayIcons()) {
			icon.setPopupMenu(null);
			systemTray.remove(icon);
		}
		if (image != null) {
			Dimension dim = systemTray.getTrayIconSize();
			TrayIcon trayIcon = new TrayIcon(image.getScaledInstance(dim.width, dim.height, Image.SCALE_SMOOTH), tooltip, menu);
			systemTray.add(trayIcon);
		}
	}

	private static class Checker extends TimerTask {

		private final InetSocketAddress host;

		Checker(InetSocketAddress host) {
			this.host = host;
		}

		@Override
		public void run() {
			try {
				LocalTime now = LocalTime.now();
				LOGGER.fine(String.format("%tT --- attempt to reach %s ---", now, host));
				URL url = new URL("http", host.getHostName(), host.getPort(), "");
				HttpURLConnection connection = (HttpURLConnection) url.openConnection();
				connection.connect();
				// set green icon
				if (SystemTray.isSupported()) {
					try {
						setIcon(green, String.format("%s reached at %tT", host, now));
					} catch (AWTException ex) {
						LOGGER.log(Level.WARNING, String.format("Could not change tray icon ! But %s was reached at %tT", host, now), ex);
					}
				}
				LOGGER.log(Level.INFO, "{0} reached !", host);
				connection.disconnect();
				cancel();
			} catch (IOException ex) {
				LOGGER.fine(ex.toString());
			}
		}
	}

	/**
	 * Starts the application. The application will display an icon in the
	 * system tray to reflect the connection status, and will start a timer task
	 * that will periodically try to connect to the specified host.
	 *
	 * @param args you can specify host name or IP, and port. If unspecified,
	 * host will be www.google.com and port will be 80
	 * .<p>
	 * <b>Exemples:</b><br>
	 * java monic.Main <i>(connect to www.google.com:80)</i><br>
	 * java monic.Main 192.168.0.3 <i>(connect to 192.168.0.3:80)</i><br>
	 * java monic.Main example.com <i>(connect to example.com:80)</i><br>
	 * java monic.Main example.com 8080 <i>(connect to example.com:8080)</i><p>
	 * Note that if an host name is given instead of an IP address, the
	 * application will try to resolve the host which may lead to a startup
	 * delay.
	 * @throws AWTException if the desktop system tray is unavailable
	 */
	public static void main(String[] args) throws AWTException {
		int port = args.length > 1 ? Integer.parseInt(args[1]) : 80;
		InetSocketAddress host = new InetSocketAddress(args.length > 0 ? args[0] : "www.google.com", port);

		if (SystemTray.isSupported()) {
			// Set default image
			setIcon(red, String.format("%s not reached yet...", host));
		} else {
			LOGGER.info("System tray not supported. Watch the logs !");
		}

		// start timer task
		timer.schedule(new Checker(host), 0, 60000);
	}
}
