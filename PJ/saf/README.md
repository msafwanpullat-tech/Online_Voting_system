# Online Voting System

A complete Java-based voting system with CSV data storage, featuring voter management, candidate management, and an admin dashboard.

## Features

### For Voters
- **Voter Login**: Secure login with Voter ID
- **Candidate Selection**: View and select from available candidates
- **Vote Casting**: One-time voting with confirmation
- **Results Viewing**: Real-time election results

### For Administrators
- **Admin Dashboard**: Complete management interface
- **Voter Management**: Add, view, and manage voters
- **Candidate Management**: Add candidates with name and party information
- **Vote Monitoring**: See who has voted and voting statistics
- **Results Analysis**: Detailed election results and turnout data

### Technical Features
- **CSV Data Storage**: All data persisted in CSV files
- **Single Java File**: Complete backend in one `VotingSystemServer.java` file
- **RESTful API**: Clean API endpoints for all operations
- **Real-time Updates**: Live data synchronization
- **Responsive Design**: Works on desktop, tablet, and mobile

## Quick Start

### Prerequisites
- Java 8 or higher installed
- Web browser (Chrome, Firefox, Safari, Edge)

### Setup Instructions

1. **Add Sample Data** (Optional)
   ```bash
   # Run this to create sample voters and candidates
   add_sample_data.bat
   ```

2. **Start the Server**
   ```bash
   # Compile and run the Java server
   run_server.bat
   ```

3. **Access the System**
   - Open your browser and go to: `http://localhost:8080`
   - The server will serve the HTML interface automatically

### Default Credentials

**Sample Voters:**
- VOT001, VOT002, VOT003, VOT004, VOT005

**Admin Access:**
- Username: `admin`
- Password: `admin123`

## File Structure

```
├── VotingSystemServer.java    # Main Java backend server
├── index.html                 # Frontend HTML/CSS/JS
├── run_server.bat            # Windows batch file to start server
├── add_sample_data.bat       # Script to add sample data
├── voters.csv                # Voter data storage
├── candidates.csv            # Candidate data storage
├── votes.csv                 # Vote records storage
└── README.md                 # This file
```

## API Endpoints

### Voter Operations
- `GET /api/voter/{voterId}` - Get voter information
- `POST /api/voter/login` - Voter login
- `POST /api/voter/add` - Add new voter (admin only)

### Candidate Operations
- `GET /api/candidates` - Get all candidates
- `POST /api/candidate/add` - Add new candidate (admin only)

### Voting Operations
- `POST /api/vote` - Cast a vote
- `GET /api/votes` - Get all votes (admin only)
- `GET /api/results` - Get election results

### Admin Operations
- `POST /api/admin/login` - Admin login
- `GET /api/voters` - Get all voters with voting status

## Data Storage

The system uses CSV files for data persistence:

### voters.csv
```
VOT001,John Smith
VOT002,Jane Doe
```

### candidates.csv
```
1,Alice Johnson,Progressive Party
2,Bob Smith,Conservative Alliance
```

### votes.csv
```
VOT001,1,2024-01-15 10:30:00
VOT002,2,2024-01-15 10:35:00
```

## Usage Guide

### For Voters
1. Click "Vote" in the navigation
2. Enter your Voter ID (e.g., VOT001)
3. Select your preferred candidate
4. Confirm your vote
5. View results

### For Administrators
1. Click "Admin" in the navigation
2. Login with admin credentials
3. Use the dashboard tabs:
   - **Voters**: Manage voter list and see voting status
   - **Candidates**: Add/manage candidates with party names
   - **Results**: View detailed election statistics

## Security Features

- **One Vote Per Voter**: System prevents duplicate voting
- **Secure Authentication**: Admin and voter login protection
- **Data Validation**: Input validation on all forms
- **CSV Backup**: All data automatically saved to CSV files

## Troubleshooting

### Server Won't Start
- Ensure Java is installed: `java -version`
- Check if port 8080 is available
- Run `javac VotingSystemServer.java` to check for compilation errors

### Connection Errors
- Verify server is running on `http://localhost:8080`
- Check browser console for error messages
- Ensure firewall allows local connections

### Data Issues
- CSV files are created automatically
- Check file permissions in the directory
- Restart server after manual CSV edits

## Development

The system is built with:
- **Backend**: Pure Java (no external dependencies)
- **Frontend**: HTML5, CSS3, JavaScript (ES6+)
- **Storage**: CSV files for simplicity
- **Architecture**: Single-threaded server with concurrent client handling

## License

This project is open source and available under the MIT License.

