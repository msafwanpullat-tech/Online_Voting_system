import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class VotingSystemServer {
    private static final int PORT = 8080;
    private static final String VOTERS_FILE = "voters.csv";
    private static final String CANDIDATES_FILE = "candidates.csv";
    private static final String VOTES_FILE = "votes.csv";
    private static final String SECTIONS_FILE = "sections.csv";
    
    // In-memory storage for quick access
    private static Map<String, Voter> voters = new ConcurrentHashMap<>();
    private static Map<Long, Candidate> candidates = new ConcurrentHashMap<>();
    private static Map<String, Vote> votes = new ConcurrentHashMap<>();
    private static Map<Long, VotingSection> sections = new ConcurrentHashMap<>();
    private static AtomicLong candidateIdCounter = new AtomicLong(1);
    private static AtomicLong sectionIdCounter = new AtomicLong(1);
    
    // Admin credentials
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "admin123";
    
    public static void main(String[] args) {
        System.out.println("Starting Voting System Server on port " + PORT);
        
        // Load existing data from CSV files
        loadDataFromCSV();
        
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started successfully!");
            System.out.println("Access the system at: http://localhost:" + PORT);
            
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }
    
    private static void loadDataFromCSV() {
        loadVoters();
        loadCandidates();
        loadVotes();
        loadSections();
        System.out.println("Data loaded: " + voters.size() + " voters, " + candidates.size() + " candidates, " + votes.size() + " votes, " + sections.size() + " sections");
    }
    
    private static void loadVoters() {
        try (BufferedReader reader = new BufferedReader(new FileReader(VOTERS_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 4) {
                    Voter voter = new Voter(parts[0], parts[1], Integer.parseInt(parts[2]), parts[3]);
                    voters.put(voter.getVoterId(), voter);
                } else if (parts.length >= 2) {
                    // Backward compatibility for old format
                    Voter voter = new Voter(parts[0], parts[1], 0, "Unknown");
                    voters.put(voter.getVoterId(), voter);
                }
            }
        } catch (IOException e) {
            System.out.println("No existing voters file found, starting fresh");
        }
    }
    
    private static void loadCandidates() {
        try (BufferedReader reader = new BufferedReader(new FileReader(CANDIDATES_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                // New format: id,name,party,age,gender,sectionId
                if (parts.length >= 6) {
                    long id = Long.parseLong(parts[0]);
                    Candidate candidate = new Candidate(id, parts[1], parts[2], Integer.parseInt(parts[3]), parts[4]);
                    try { candidate.setSectionId(Long.parseLong(parts[5])); } catch (NumberFormatException ignore) {}
                    candidates.put(candidate.getId(), candidate);
                    candidateIdCounter.set(Math.max(candidateIdCounter.get(), candidate.getId() + 1));
                } else if (parts.length >= 3) {
                    // Backward compatibility for old format
                    Candidate candidate = new Candidate(Long.parseLong(parts[0]), parts[1], parts[2], 0, "Unknown");
                    candidates.put(candidate.getId(), candidate);
                    candidateIdCounter.set(Math.max(candidateIdCounter.get(), candidate.getId() + 1));
                }
            }
        } catch (IOException e) {
            System.out.println("No existing candidates file found, starting fresh");
        }
    }
    
    private static void loadVotes() {
        try (BufferedReader reader = new BufferedReader(new FileReader(VOTES_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                // Support two formats:
                // old: voterId,candidateId,timestamp
                // new: voterId,candidateId,sectionId,timestamp
                if (parts.length >= 3) {
                    String voterId = parts[0];
                    long candidateId = 0;
                    long sectionId = 0;
                    String timestamp = "";
                    try {
                        candidateId = Long.parseLong(parts[1]);
                    } catch (NumberFormatException ignore) {}

                    if (parts.length >= 4) {
                        try { sectionId = Long.parseLong(parts[2]); } catch (NumberFormatException ignore) {}
                        timestamp = parts[3];
                    } else {
                        // fallback
                        timestamp = parts[2];
                    }

                    Vote vote = new Vote(voterId, candidateId, sectionId, timestamp);
                    votes.put(vote.getVoterId(), vote);
                }
            }
        } catch (IOException e) {
            System.out.println("No existing votes file found, starting fresh");
        }
    }
    
    private static void saveVoters() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(VOTERS_FILE))) {
            for (Voter voter : voters.values()) {
                writer.println(voter.getVoterId() + "," + voter.getName() + "," + voter.getAge() + "," + voter.getGender());
            }
        } catch (IOException e) {
            System.err.println("Error saving voters: " + e.getMessage());
        }
    }
    
    private static void saveCandidates() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(CANDIDATES_FILE))) {
            for (Candidate candidate : candidates.values()) {
                // Write format: id,name,party,age,gender,sectionId
                writer.println(candidate.getId() + "," + candidate.getName() + "," + candidate.getParty() + "," + candidate.getAge() + "," + candidate.getGender() + "," + candidate.getSectionId());
            }
        } catch (IOException e) {
            System.err.println("Error saving candidates: " + e.getMessage());
        }
    }
    
    private static void saveVotes() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(VOTES_FILE))) {
            for (Vote vote : votes.values()) {
                // Write format: voterId,candidateId,sectionId,timestamp
                writer.println(vote.getVoterId() + "," + vote.getCandidateId() + "," + vote.getSectionId() + "," + vote.getTimestamp());
            }
        } catch (IOException e) {
            System.err.println("Error saving votes: " + e.getMessage());
        }
    }
    
    private static void loadSections() {
        try (BufferedReader reader = new BufferedReader(new FileReader(SECTIONS_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 5) {
                    VotingSection section = new VotingSection(Long.parseLong(parts[0]), parts[1], parts[2], parts[3], parts[4]);
                    sections.put(section.getId(), section);
                    sectionIdCounter.set(Math.max(sectionIdCounter.get(), section.getId() + 1));
                }
            }
        } catch (IOException e) {
            System.out.println("No existing sections file found, starting fresh");
        }
    }
    
    private static void saveSections() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(SECTIONS_FILE))) {
            for (VotingSection section : sections.values()) {
                writer.println(section.getId() + "," + section.getName() + "," + section.getDescription() + "," + section.getStartDate() + "," + section.getEndDate());
            }
        } catch (IOException e) {
            System.err.println("Error saving sections: " + e.getMessage());
        }
    }
    
    static class ClientHandler implements Runnable {
        private Socket clientSocket;
        
        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }
        
        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                 PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {
                // Read request line
                String requestLine = in.readLine();
                if (requestLine == null) {
                    return;
                }

                // Read headers
                Map<String, String> headers = new HashMap<>();
                String line;
                int contentLength = 0;
                StringBuilder rawRequest = new StringBuilder();
                rawRequest.append(requestLine).append("\r\n");
                while ((line = in.readLine()) != null && !line.isEmpty()) {
                    rawRequest.append(line).append("\r\n");
                    int idx = line.indexOf(":");
                    if (idx > 0) {
                        String key = line.substring(0, idx).trim().toLowerCase();
                        String value = line.substring(idx + 1).trim();
                        headers.put(key, value);
                        if (key.equals("content-length")) {
                            try { contentLength = Integer.parseInt(value); } catch (NumberFormatException ignore) {}
                        }
                    }
                }
                // Blank line between headers and body
                rawRequest.append("\r\n");

                // Read body if present
                String body = "";
                if (contentLength > 0) {
                    char[] buf = new char[contentLength];
                    int read = 0;
                    while (read < contentLength) {
                        int r = in.read(buf, read, contentLength - read);
                        if (r == -1) break;
                        read += r;
                    }
                    body = new String(buf, 0, read);
                    rawRequest.append(body);
                }

                String response = handleRequest(rawRequest.toString());
                out.print(response);
                out.flush();
            } catch (IOException e) {
                System.err.println("Client handler error: " + e.getMessage());
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    System.err.println("Error closing client socket: " + e.getMessage());
                }
            }
        }
        
        private String handleRequest(String request) {
            try {
                String[] parts = request.split(" ");
                if (parts.length < 2) {
                    return "HTTP/1.1 400 Bad Request\r\n\r\nInvalid request";
                }

                String method = parts[0];
                String path = parts[1];
                
                if (method.equals("OPTIONS")) {
                    return "HTTP/1.1 204 No Content\r\n" +
                           "Access-Control-Allow-Origin: *\r\n" +
                           "Access-Control-Allow-Methods: GET, POST, OPTIONS\r\n" +
                           "Access-Control-Allow-Headers: Content-Type\r\n\r\n";
                } else if (method.equals("GET")) {
                    return handleGetRequest(path);
                } else if (method.equals("POST")) {
                    return handlePostRequest(path, request);
                } else {
                    return "HTTP/1.1 405 Method Not Allowed\r\n\r\nMethod not allowed";
                }
            } catch (Exception e) {
                return "HTTP/1.1 500 Internal Server Error\r\n\r\n" + e.getMessage();
            }
        }
        
        private String handleGetRequest(String path) {
            // Support query params (e.g. /api/results?sectionId=1)
            String query = null;
            if (path.contains("?")) {
                int idx = path.indexOf('?');
                query = path.substring(idx + 1);
                path = path.substring(0, idx);
            }

            if (path.startsWith("/api/voters")) {
                return getVoters();
            } else if (path.startsWith("/api/candidates")) {
                Long sectionId = null;
                if (query != null) {
                    String[] pairs = query.split("&");
                    for (String p : pairs) {
                        String[] kv = p.split("=");
                        if (kv.length == 2 && kv[0].equals("sectionId")) {
                            try { sectionId = Long.parseLong(kv[1]); } catch (NumberFormatException ignore) {}
                        }
                    }
                }
                return getCandidates(sectionId);
            } else if (path.startsWith("/api/votes")) {
                return getVotes();
            } else if (path.startsWith("/api/results")) {
                Long sectionId = null;
                if (query != null) {
                    String[] pairs = query.split("&");
                    for (String p : pairs) {
                        String[] kv = p.split("=");
                        if (kv.length == 2 && kv[0].equals("sectionId")) {
                            try { sectionId = Long.parseLong(kv[1]); } catch (NumberFormatException ignore) {}
                        }
                    }
                }
                return getResults(sectionId);
            } else if (path.startsWith("/api/sections")) {
                return getSections();
            } else if (path.startsWith("/api/voter/")) {
                String voterId = path.substring("/api/voter/".length());
                return getVoter(voterId);
            } else {
                return serveStaticFile(path);
            }
        }
        
        private String handlePostRequest(String path, String request) {
            String body = extractBody(request);
            
            if (path.equals("/api/voter/login")) {
                return loginVoter(body);
            } else if (path.equals("/api/admin/login")) {
                return loginAdmin(body);
            } else if (path.equals("/api/vote")) {
                return castVote(body);
            } else if (path.equals("/api/voter/add")) {
                return addVoter(body);
            } else if (path.equals("/api/candidate/add")) {
                return addCandidate(body);
            } else if (path.equals("/api/voter/delete")) {
                return deleteVoter(body);
            } else if (path.equals("/api/candidate/delete")) {
                return deleteCandidate(body);
            } else if (path.equals("/api/sections/create")) {
                return createSection(body);
            } else if (path.equals("/api/sections/delete")) {
                return deleteSection(body);
            } else {
                return "HTTP/1.1 404 Not Found\r\n\r\nEndpoint not found";
            }
        }
        
        private String extractBody(String request) {
            String[] lines = request.split("\r\n");
            for (int i = 0; i < lines.length; i++) {
                if (lines[i].isEmpty()) {
                    StringBuilder body = new StringBuilder();
                    for (int j = i + 1; j < lines.length; j++) {
                        body.append(lines[j]);
                        if (j < lines.length - 1) body.append("\r\n");
                    }
                    return body.toString();
                }
            }
            return "";
        }
        
        private String getVoters() {
            StringBuilder json = new StringBuilder();
            json.append("HTTP/1.1 200 OK\r\n");
            json.append("Content-Type: application/json\r\n");
            json.append("Access-Control-Allow-Origin: *\r\n\r\n");
            json.append("{\"voters\":[");
            
            boolean first = true;
            for (Voter voter : voters.values()) {
                if (!first) json.append(",");
                boolean hasVoted = votes.containsKey(voter.getVoterId());
                json.append("{\"voterId\":\"").append(voter.getVoterId()).append("\",");
                json.append("\"name\":\"").append(voter.getName()).append("\",");
                json.append("\"age\":").append(voter.getAge()).append(",");
                json.append("\"gender\":\"").append(voter.getGender()).append("\",");
                json.append("\"voted\":").append(hasVoted);
                json.append("}");
                first = false;
            }
            json.append("]}");
            return json.toString();
        }

        private String getCandidates() {
            StringBuilder json = new StringBuilder();
            json.append("HTTP/1.1 200 OK\r\n");
            json.append("Content-Type: application/json\r\n");
            json.append("Access-Control-Allow-Origin: *\r\n\r\n");
            json.append("{\"candidates\":[");

            boolean first = true;
            for (Candidate candidate : candidates.values()) {
                if (!first) json.append(",");
                json.append("{\"id\":").append(candidate.getId()).append(",");
                json.append("\"name\":\"").append(candidate.getName()).append("\",");
                json.append("\"party\":\"").append(candidate.getParty()).append("\",");
                json.append("\"age\":").append(candidate.getAge()).append(",");
                json.append("\"gender\":\"").append(candidate.getGender()).append("\"}");
                first = false;
            }
            json.append("]}");
            return json.toString();
        }

        private String getCandidates(Long sectionId) {
            StringBuilder json = new StringBuilder();
            json.append("HTTP/1.1 200 OK\r\n");
            json.append("Content-Type: application/json\r\n");
            json.append("Access-Control-Allow-Origin: *\r\n\r\n");
            json.append("{\"candidates\":[");

            boolean first = true;
            for (Candidate candidate : candidates.values()) {
                if (sectionId != null && sectionId > 0 && candidate.getSectionId() != sectionId) continue;
                if (!first) json.append(",");
                json.append("{\"id\":").append(candidate.getId()).append(",");
                json.append("\"name\":\"").append(candidate.getName()).append("\",");
                json.append("\"party\":\"").append(candidate.getParty()).append("\",");
                json.append("\"age\":").append(candidate.getAge()).append(",");
                json.append("\"gender\":\"").append(candidate.getGender()).append("\",");
                json.append("\"sectionId\":").append(candidate.getSectionId());
                json.append("}");
                first = false;
            }
            json.append("]}");
            return json.toString();
        }
        
        private String getResults(Long sectionId) {
            Map<Long, Integer> voteCounts = new HashMap<>();
            int total = 0;
            for (Vote vote : votes.values()) {
                if (sectionId != null && sectionId > 0) {
                    if (vote.getSectionId() != sectionId) continue;
                }
                voteCounts.put(vote.getCandidateId(), voteCounts.getOrDefault(vote.getCandidateId(), 0) + 1);
                total++;
            }

            StringBuilder json = new StringBuilder();
            json.append("HTTP/1.1 200 OK\r\n");
            json.append("Content-Type: application/json\r\n");
            json.append("Access-Control-Allow-Origin: *\r\n\r\n");
            json.append("{\"candidates\":[");

            boolean first = true;
            for (Candidate candidate : candidates.values()) {
                if (!first) json.append(",");
                int voteCount = voteCounts.getOrDefault(candidate.getId(), 0);
                json.append("{\"name\":\"").append(candidate.getName()).append("\",");
                json.append("\"party\":\"").append(candidate.getParty()).append("\",");
                json.append("\"votes\":").append(voteCount).append("}");
                first = false;
            }
            json.append("],\"totalVotes\":").append(total);
            json.append(",\"totalCandidates\":").append(candidates.size());
            json.append("}");
            return json.toString();
        }

        private String getVotes() {
            StringBuilder json = new StringBuilder();
            json.append("HTTP/1.1 200 OK\r\n");
            json.append("Content-Type: application/json\r\n");
            json.append("Access-Control-Allow-Origin: *\r\n\r\n");
            json.append("{\"votes\":[");

            boolean first = true;
            for (Vote vote : votes.values()) {
                if (!first) json.append(",");
                json.append("{\"voterId\":\"").append(vote.getVoterId()).append("\",");
                json.append("\"candidateId\":").append(vote.getCandidateId()).append(",");
                json.append("\"sectionId\":").append(vote.getSectionId()).append(",");
                json.append("\"timestamp\":\"").append(vote.getTimestamp()).append("\"}");
                first = false;
            }
            json.append("]}");
            return json.toString();
        }
        
        private String getResults() {
            Map<Long, Integer> voteCounts = new HashMap<>();
            for (Vote vote : votes.values()) {
                voteCounts.put(vote.getCandidateId(), voteCounts.getOrDefault(vote.getCandidateId(), 0) + 1);
            }
            
            StringBuilder json = new StringBuilder();
            json.append("HTTP/1.1 200 OK\r\n");
            json.append("Content-Type: application/json\r\n");
            json.append("Access-Control-Allow-Origin: *\r\n\r\n");
            json.append("{\"candidates\":[");
            
            boolean first = true;
            for (Candidate candidate : candidates.values()) {
                if (!first) json.append(",");
                int voteCount = voteCounts.getOrDefault(candidate.getId(), 0);
                json.append("{\"name\":\"").append(candidate.getName()).append("\",");
                json.append("\"party\":\"").append(candidate.getParty()).append("\",");
                json.append("\"votes\":").append(voteCount).append("}");
                first = false;
            }
            json.append("],\"totalVotes\":").append(votes.size());
            json.append(",\"totalCandidates\":").append(candidates.size());
            json.append("}");
            return json.toString();
        }
        
        private String getVoter(String voterId) {
            Voter voter = voters.get(voterId);
            if (voter != null) {
                boolean hasVoted = votes.containsKey(voter.getVoterId());
                return "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nAccess-Control-Allow-Origin: *\r\n\r\n" +
                       "{\"success\":true,\"voter\":{\"voterId\":\"" + voter.getVoterId() + "\",\"name\":\"" + voter.getName() + "\",\"age\":\"" + voter.getAge() + "\",\"gender\":\"" + voter.getGender() + "\",\"voted\":" + hasVoted + "}}";
            } else {
                return "HTTP/1.1 404 Not Found\r\nContent-Type: application/json\r\nAccess-Control-Allow-Origin: *\r\n\r\n" +
                       "{\"success\":false,\"message\":\"Voter not found\"}";
            }
        }
        
        private String loginVoter(String body) {
            String voterId = extractParameter(body, "voterId");
            if (voterId == null) {
                return "HTTP/1.1 400 Bad Request\r\nContent-Type: application/json\r\nAccess-Control-Allow-Origin: *\r\n\r\n" +
                       "{\"success\":false,\"message\":\"Voter ID required\"}";
            }
            
            Voter voter = voters.get(voterId);
            if (voter != null) {
                return "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nAccess-Control-Allow-Origin: *\r\n\r\n" +
                       "{\"success\":true,\"voter\":{\"voterId\":\"" + voter.getVoterId() + "\",\"name\":\"" + voter.getName() + "\"}}";
            } else {
                return "HTTP/1.1 404 Not Found\r\nContent-Type: application/json\r\nAccess-Control-Allow-Origin: *\r\n\r\n" +
                       "{\"success\":false,\"message\":\"Invalid Voter ID\"}";
            }
        }
        
        private String loginAdmin(String body) {
            String username = extractParameter(body, "username");
            String password = extractParameter(body, "password");
            
            if (ADMIN_USERNAME.equals(username) && ADMIN_PASSWORD.equals(password)) {
                return "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nAccess-Control-Allow-Origin: *\r\n\r\n" +
                       "{\"success\":true,\"message\":\"Admin login successful\"}";
            } else {
                return "HTTP/1.1 401 Unauthorized\r\nContent-Type: application/json\r\nAccess-Control-Allow-Origin: *\r\n\r\n" +
                       "{\"success\":false,\"message\":\"Invalid credentials\"}";
            }
        }
        
        private String castVote(String body) {
            String voterId = extractParameter(body, "voterId");
            String candidateIdStr = extractParameter(body, "candidateId");
            String sectionIdStr = extractParameter(body, "sectionId");
            
            if (voterId == null || candidateIdStr == null) {
                return "HTTP/1.1 400 Bad Request\r\nContent-Type: application/json\r\nAccess-Control-Allow-Origin: *\r\n\r\n" +
                       "{\"success\":false,\"message\":\"Voter ID and Candidate ID required\"}";
            }
            
            if (votes.containsKey(voterId)) {
                return "HTTP/1.1 400 Bad Request\r\nContent-Type: application/json\r\nAccess-Control-Allow-Origin: *\r\n\r\n" +
                       "{\"success\":false,\"message\":\"Voter has already voted\"}";
            }
            
            try {
                long candidateId = Long.parseLong(candidateIdStr);
                long sectionId = 0;
                if (sectionIdStr != null) {
                    try { sectionId = Long.parseLong(sectionIdStr); } catch (NumberFormatException ignore) {}
                }
                if (!candidates.containsKey(candidateId)) {
                    return "HTTP/1.1 404 Not Found\r\nContent-Type: application/json\r\nAccess-Control-Allow-Origin: *\r\n\r\n" +
                           "{\"success\":false,\"message\":\"Candidate not found\"}";
                }
                
                Vote vote = new Vote(voterId, candidateId, sectionId, new Date().toString());
                votes.put(voterId, vote);
                saveVotes();
                
                return "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nAccess-Control-Allow-Origin: *\r\n\r\n" +
                       "{\"success\":true,\"message\":\"Vote recorded successfully\"}";
            } catch (NumberFormatException e) {
                return "HTTP/1.1 400 Bad Request\r\nContent-Type: application/json\r\nAccess-Control-Allow-Origin: *\r\n\r\n" +
                       "{\"success\":false,\"message\":\"Invalid candidate ID\"}";
            }
        }
        
        private String addVoter(String body) {
            String voterId = extractParameter(body, "voterId");
            String name = extractParameter(body, "name");
            String ageStr = extractParameter(body, "age");
            String gender = extractParameter(body, "gender");
            
            if (voterId == null || name == null || ageStr == null || gender == null) {
                return "HTTP/1.1 400 Bad Request\r\nContent-Type: application/json\r\nAccess-Control-Allow-Origin: *\r\n\r\n" +
                       "{\"success\":false,\"message\":\"Voter ID, name, age, and gender required\"}";
            }
            
            if (voters.containsKey(voterId)) {
                return "HTTP/1.1 400 Bad Request\r\nContent-Type: application/json\r\nAccess-Control-Allow-Origin: *\r\n\r\n" +
                       "{\"success\":false,\"message\":\"Voter ID already exists\"}";
            }
            
            try {
                int age = Integer.parseInt(ageStr);
                Voter voter = new Voter(voterId, name, age, gender);
                voters.put(voterId, voter);
                saveVoters();
                
                return "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nAccess-Control-Allow-Origin: *\r\n\r\n" +
                       "{\"success\":true,\"message\":\"Voter added successfully\"}";
            } catch (NumberFormatException e) {
                return "HTTP/1.1 400 Bad Request\r\nContent-Type: application/json\r\nAccess-Control-Allow-Origin: *\r\n\r\n" +
                       "{\"success\":false,\"message\":\"Invalid age format\"}";
            }
        }
        
        private String addCandidate(String body) {
            String name = extractParameter(body, "name");
            String party = extractParameter(body, "party");
            String ageStr = extractParameter(body, "age");
            String gender = extractParameter(body, "gender");
            String sectionIdStr = extractParameter(body, "sectionId");
            
            if (name == null || party == null || ageStr == null || gender == null) {
                return "HTTP/1.1 400 Bad Request\r\nContent-Type: application/json\r\nAccess-Control-Allow-Origin: *\r\n\r\n" +
                       "{\"success\":false,\"message\":\"Name, party, age, and gender required\"}";
            }
            
            try {
                int age = Integer.parseInt(ageStr);
                long id = candidateIdCounter.getAndIncrement();
                Candidate candidate = new Candidate(id, name, party, age, gender);
                if (sectionIdStr != null) {
                    try { candidate.setSectionId(Long.parseLong(sectionIdStr)); } catch (NumberFormatException ignore) {}
                }
                candidates.put(id, candidate);
                saveCandidates();
                
                return "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nAccess-Control-Allow-Origin: *\r\n\r\n" +
                       "{\"success\":true,\"message\":\"Candidate added successfully\",\"candidateId\":" + id + "}";
            } catch (NumberFormatException e) {
                return "HTTP/1.1 400 Bad Request\r\nContent-Type: application/json\r\nAccess-Control-Allow-Origin: *\r\n\r\n" +
                       "{\"success\":false,\"message\":\"Invalid age format\"}";
            }
        }
        
        private String deleteVoter(String body) {
            String voterId = extractParameter(body, "voterId");
            
            if (voterId == null) {
                return "HTTP/1.1 400 Bad Request\r\nContent-Type: application/json\r\nAccess-Control-Allow-Origin: *\r\n\r\n" +
                       "{\"success\":false,\"message\":\"Voter ID required\"}";
            }
            
            if (!voters.containsKey(voterId)) {
                return "HTTP/1.1 404 Not Found\r\nContent-Type: application/json\r\nAccess-Control-Allow-Origin: *\r\n\r\n" +
                       "{\"success\":false,\"message\":\"Voter not found\"}";
            }
            
            // Check if voter has voted
            if (votes.containsKey(voterId)) {
                return "HTTP/1.1 400 Bad Request\r\nContent-Type: application/json\r\nAccess-Control-Allow-Origin: *\r\n\r\n" +
                       "{\"success\":false,\"message\":\"Cannot delete voter who has already voted\"}";
            }
            
            voters.remove(voterId);
            saveVoters();
            
            return "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nAccess-Control-Allow-Origin: *\r\n\r\n" +
                   "{\"success\":true,\"message\":\"Voter deleted successfully\"}";
        }
        
        private String deleteCandidate(String body) {
            String candidateIdStr = extractParameter(body, "candidateId");
            
            if (candidateIdStr == null) {
                return "HTTP/1.1 400 Bad Request\r\nContent-Type: application/json\r\nAccess-Control-Allow-Origin: *\r\n\r\n" +
                       "{\"success\":false,\"message\":\"Candidate ID required\"}";
            }
            
            try {
                long candidateId = Long.parseLong(candidateIdStr);
                
                if (!candidates.containsKey(candidateId)) {
                    return "HTTP/1.1 404 Not Found\r\nContent-Type: application/json\r\nAccess-Control-Allow-Origin: *\r\n\r\n" +
                           "{\"success\":false,\"message\":\"Candidate not found\"}";
                }
                
                // Check if candidate has received votes
                boolean hasVotes = false;
                for (Vote vote : votes.values()) {
                    if (vote.getCandidateId() == candidateId) {
                        hasVotes = true;
                        break;
                    }
                }
                
                if (hasVotes) {
                    return "HTTP/1.1 400 Bad Request\r\nContent-Type: application/json\r\nAccess-Control-Allow-Origin: *\r\n\r\n" +
                           "{\"success\":false,\"message\":\"Cannot delete candidate who has received votes\"}";
                }
                
                candidates.remove(candidateId);
                saveCandidates();
                
                return "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nAccess-Control-Allow-Origin: *\r\n\r\n" +
                       "{\"success\":true,\"message\":\"Candidate deleted successfully\"}";
            } catch (NumberFormatException e) {
                return "HTTP/1.1 400 Bad Request\r\nContent-Type: application/json\r\nAccess-Control-Allow-Origin: *\r\n\r\n" +
                       "{\"success\":false,\"message\":\"Invalid candidate ID\"}";
            }
        }
        
        private String getSections() {
            StringBuilder json = new StringBuilder();
            json.append("HTTP/1.1 200 OK\r\n");
            json.append("Content-Type: application/json\r\n");
            json.append("Access-Control-Allow-Origin: *\r\n\r\n");
            json.append("{\"sections\":[");
            
            boolean first = true;
            for (VotingSection section : sections.values()) {
                if (!first) json.append(",");
                json.append("{\"id\":").append(section.getId()).append(",");
                json.append("\"name\":\"").append(section.getName()).append("\",");
                json.append("\"description\":\"").append(section.getDescription()).append("\",");
                json.append("\"startDate\":\"").append(section.getStartDate()).append("\",");
                json.append("\"endDate\":\"").append(section.getEndDate()).append("\",");
                json.append("\"status\":\"").append(section.getStatus()).append("\",");
                json.append("\"voteCount\":").append(section.getVoteCount());
                json.append("}");
                first = false;
            }
            json.append("]}");
            return json.toString();
        }
        
        private String createSection(String body) {
            String name = extractParameter(body, "name");
            String description = extractParameter(body, "description");
            String startDate = extractParameter(body, "startDate");
            String endDate = extractParameter(body, "endDate");
            
            if (name == null || startDate == null || endDate == null) {
                return "HTTP/1.1 400 Bad Request\r\nContent-Type: application/json\r\nAccess-Control-Allow-Origin: *\r\n\r\n" +
                       "{\"success\":false,\"message\":\"Name, start date, and end date required\"}";
            }
            
            if (description == null) description = "";
            
            long id = sectionIdCounter.getAndIncrement();
            VotingSection section = new VotingSection(id, name, description, startDate, endDate);
            sections.put(id, section);
            saveSections();
            
            return "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nAccess-Control-Allow-Origin: *\r\n\r\n" +
                   "{\"success\":true,\"message\":\"Voting section created successfully\",\"sectionId\":" + id + "}";
        }
        
        private String deleteSection(String body) {
            String sectionIdStr = extractParameter(body, "sectionId");
            
            if (sectionIdStr == null) {
                return "HTTP/1.1 400 Bad Request\r\nContent-Type: application/json\r\nAccess-Control-Allow-Origin: *\r\n\r\n" +
                       "{\"success\":false,\"message\":\"Section ID required\"}";
            }
            
            try {
                long sectionId = Long.parseLong(sectionIdStr);
                
                if (!sections.containsKey(sectionId)) {
                    return "HTTP/1.1 404 Not Found\r\nContent-Type: application/json\r\nAccess-Control-Allow-Origin: *\r\n\r\n" +
                           "{\"success\":false,\"message\":\"Section not found\"}";
                }
                
                // Delete all votes for this section (if any)
                votes.entrySet().removeIf(entry -> {
                    Vote v = entry.getValue();
                    return v.getSectionId() == sectionId;
                });
                saveVotes();
                
                sections.remove(sectionId);
                saveSections();
                
                return "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nAccess-Control-Allow-Origin: *\r\n\r\n" +
                       "{\"success\":true,\"message\":\"Voting section deleted successfully\"}";
            } catch (NumberFormatException e) {
                return "HTTP/1.1 400 Bad Request\r\nContent-Type: application/json\r\nAccess-Control-Allow-Origin: *\r\n\r\n" +
                       "{\"success\":false,\"message\":\"Invalid section ID\"}";
            }
        }
        
        private String serveStaticFile(String path) {
            if (path.equals("/") || path.equals("/index.html")) {
                return serveHTML();
            } else {
                return "HTTP/1.1 404 Not Found\r\n\r\nFile not found";
            }
        }
        
        private String serveHTML() {
            try {
                StringBuilder html = new StringBuilder();
                BufferedReader reader = new BufferedReader(new FileReader("index.html"));
                String line;
                while ((line = reader.readLine()) != null) {
                    html.append(line).append("\r\n");
                }
                reader.close();
                
                return "HTTP/1.1 200 OK\r\nContent-Type: text/html\r\n\r\n" + html.toString();
            } catch (IOException e) {
                return "HTTP/1.1 500 Internal Server Error\r\n\r\nError loading HTML file";
            }
        }
        
        private String extractParameter(String body, String paramName) {
            String[] pairs = body.split("&");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=");
                if (keyValue.length == 2 && keyValue[0].equals(paramName)) {
                    try {
                        return java.net.URLDecoder.decode(keyValue[1], "UTF-8");
                    } catch (Exception e) {
                        return keyValue[1];
                    }
                }
            }
            return null;
        }
    }
    
    // Data classes
    static class Voter {
        private String voterId;
        private String name;
        private int age;
        private String gender;
        
        public Voter(String voterId, String name, int age, String gender) {
            this.voterId = voterId;
            this.name = name;
            this.age = age;
            this.gender = gender;
        }
        
        public String getVoterId() { return voterId; }
        public String getName() { return name; }
        public int getAge() { return age; }
        public String getGender() { return gender; }
    }
    
    static class Candidate {
        private long id;
        private String name;
        private String party;
        private int age;
        private String gender;
        private long sectionId;
        
        public Candidate(long id, String name, String party, int age, String gender) {
            this.id = id;
            this.name = name;
            this.party = party;
            this.age = age;
            this.gender = gender;
            this.sectionId = 0;
        }
        
        public long getId() { return id; }
        public String getName() { return name; }
        public String getParty() { return party; }
        public int getAge() { return age; }
        public String getGender() { return gender; }
        public long getSectionId() { return sectionId; }
        public void setSectionId(long sectionId) { this.sectionId = sectionId; }
    }
    
    static class Vote {
        private String voterId;
        private long candidateId;
        private long sectionId;
        private String timestamp;
        
        public Vote(String voterId, long candidateId, String timestamp) {
            this.voterId = voterId;
            this.candidateId = candidateId;
            this.sectionId = 0;
            this.timestamp = timestamp;
        }

        public Vote(String voterId, long candidateId, long sectionId, String timestamp) {
            this.voterId = voterId;
            this.candidateId = candidateId;
            this.sectionId = sectionId;
            this.timestamp = timestamp;
        }

        public String getVoterId() { return voterId; }
        public long getCandidateId() { return candidateId; }
        public long getSectionId() { return sectionId; }
        public String getTimestamp() { return timestamp; }
    }
    
    static class VotingSection {
        private long id;
        private String name;
        private String description;
        private String startDate;
        private String endDate;
        private String status;
        private int voteCount;
        
        public VotingSection(long id, String name, String description, String startDate, String endDate) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.startDate = startDate;
            this.endDate = endDate;
            this.status = "Active";
            this.voteCount = 0;
        }
        
        public long getId() { return id; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public String getStartDate() { return startDate; }
        public String getEndDate() { return endDate; }
        public String getStatus() { return status; }
        public int getVoteCount() { return voteCount; }
        public void setVoteCount(int voteCount) { this.voteCount = voteCount; }
    }
}
