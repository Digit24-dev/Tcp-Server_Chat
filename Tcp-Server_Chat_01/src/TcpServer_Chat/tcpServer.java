package TcpServer_Chat;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class tcpServer {
	public static void main(String[] args) {
		ServerSocket ssock = null;
		HashMap<String, PrintWriter> hm;
		
		try {
			int portNum = 5000;
			ssock = new ServerSocket(portNum);
			System.out.println("--- 서버 Listening --- port number:"+portNum);
			
			hm = new HashMap<String, PrintWriter>();
			
			while(true) {
				Socket csock = ssock.accept();
				
				ServerThread chat_thread = new ServerThread(csock, hm);
				chat_thread.start();
			}
			
		}catch(Exception e) {
			System.out.println(e);
		}
		
	}
}

class ServerThread extends Thread{
	private Socket sock;
	private String id;
	
	private InputStream is;
	private InputStreamReader isr;
	private BufferedReader br;
	
	private OutputStream os;
	private OutputStreamWriter osw;
	private PrintWriter pw;
	
	// 파일 전송 객체 생성
	private DataOutputStream dos = new DataOutputStream(os);
	
	private FileInputStream fin = null;
	private String file_dir = "C:\\Users\\legoj\\eclipse-workspace\\TcpServer\\";
	byte[] buffer = new byte[1024];
	int length;
	
	private HashMap<String, PrintWriter> hm;
	boolean initFlag = false;
	private boolean fileFlag = false;
	
	SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	String format_time1 = format1.format(System.currentTimeMillis());
	
	
	public ServerThread(Socket sock, HashMap<String, PrintWriter> hm) {
		this.sock = sock;
		this.hm = hm;
		
		try {
			os = sock.getOutputStream();
			osw = new OutputStreamWriter(os, "UTF-8");
			pw = new PrintWriter(osw);
			
			is = sock.getInputStream();
			isr = new InputStreamReader(is, "UTF-8");
			br = new BufferedReader(isr);
			
			id = br.readLine();
			
			System.out.println("[" + id + "] is active");
			
			synchronized(hm) {
				hm.put(this.id, pw);
			}
			
			pw.println("[" + id + "]==> connection is successful!!!");
			pw.flush();
			
		} catch(Exception e) {
			System.out.println(e);
		}
	}

	public void run() {
		String line = null;
		String temp = null;
		try {
			while((line = br.readLine()) != null) {
				
				// 종료
				if(line.equals("/quit")) 
					break;
				
				// ********* 귓속말 *********
				else if(line.contains("/w")) {
					String[] msg = line.split(" ");
					String talk = "";
					
					for(int i=2; i<msg.length; i++) {
						talk = talk + " " + msg[i];
					}
					// 로그
					System.out.println("[" + format_time1 + "]" + "(whisper)" + id + ":" + talk);
					// 전송
					whisper(id, msg[1], talk);
				}
				
				// ********* 파일 보내기 **********
				else if(line.contains("/getfile")) {
					System.out.println("파일 전송중...");
					
					String[] msg = line.split(" ");
					String fileName = msg[1];
					
					if(new File(file_dir+fileName).isFile()) {
						fileTransfer(id, fileName);
						System.out.println("작업 완료.");
					}else {
						sendCommand("Server",id,"파일이 존재하지 않습니다.");
						System.out.println(id + "--> 요청거부 : 파일 존재하지 않음.");
					}
				}
				
				// ********* 일반 채팅 *********
				else {
					System.out.println("[" + format_time1 + "]" + "[ALL]" + id + " : " + line);
					broadcast("[" + format_time1 + "]" + "[ALL]" + id + ": " + line);
				}
			}
		} catch(Exception e) {
			System.out.println(e);
		} finally {
			synchronized(hm) {
				hm.remove(id);
			}
			System.out.println(id + " connection is closed ........[O.K]");
			try {
				if (sock != null)
					sock.close();
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void broadcast(String msg) {
		synchronized(hm) {
			Collection<PrintWriter> collection = hm.values();
			
			Iterator<PrintWriter> iter = collection.iterator();
			while(iter.hasNext()) {
				PrintWriter pw = (PrintWriter) iter.next();
				pw.println(msg);
				pw.flush();
			}
		}
	}
	
	public void sendCommand(String fromID, String toID, String command) {
		pw = hm.get(toID);
		
		if(pw != null) {
			pw.println("[" + format_time1 + "]" + fromID + ":" + command);
			pw.flush();
		}
	}
	
	public void whisper(String fromID, String toID, String command) {
		pw = hm.get(toID);
		
		if(pw != null) {
			pw.println("[" + format_time1 + "]" + "(whisper)" + fromID + ":" + command);
			pw.flush();
		}
	}
	
	public void fileTransfer(String toID, String name) throws IOException {
		dos = new DataOutputStream(os);
		pw = hm.get(toID);
		File file = null;
		System.out.println("hi");
		if(pw != null) {
			pw.println("/fileTransfer.. " + name);
			pw.flush();
		}
		try {
			file = new File(file_dir + name);
			fin = new FileInputStream(file);
			dos.writeLong(file.length());
			
			while((length = fin.read(buffer))!=-1) {
				dos.write(buffer, 0, length);
				dos.flush();
			}
			
		}catch(Exception e) {
			e.printStackTrace();
		}finally {
			fin.close();
		}
	}
}
