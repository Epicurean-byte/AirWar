package edu.hitsz.aircraftwar.android.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import edu.hitsz.aircraftwar.android.R;
import edu.hitsz.aircraftwar.android.network.ConnectionTester;
import edu.hitsz.aircraftwar.android.network.LanServerScanner;
import edu.hitsz.aircraftwar.android.network.ServerConfigManager;

/**
 * Fragment for configuring server connection settings.
 * Allows users to input and validate server IP address and port.
 */
public class SettingsFragment extends Fragment {
    
    private static final int SCAN_PORT = 18080;
    
    private EditText editServerIp;
    private EditText editServerPort;
    private Button btnScanLan;
    private Button btnTestConnection;
    private Button btnSave;
    private ProgressBar progressScanLan;
    private ProgressBar progressTestConnection;
    private TextView textScanProgress;
    private TextView textConnectionResult;
    private RecyclerView recyclerDiscoveredServers;
    private ServerConfigManager configManager;
    private ConnectionTester connectionTester;
    private LanServerScanner lanServerScanner;
    private DiscoveredServersAdapter serversAdapter;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        
        // Initialize views
        editServerIp = view.findViewById(R.id.edit_server_ip);
        editServerPort = view.findViewById(R.id.edit_server_port);
        btnScanLan = view.findViewById(R.id.btn_scan_lan);
        btnTestConnection = view.findViewById(R.id.btn_test_connection);
        btnSave = view.findViewById(R.id.btn_save);
        progressScanLan = view.findViewById(R.id.progress_scan_lan);
        progressTestConnection = view.findViewById(R.id.progress_test_connection);
        textScanProgress = view.findViewById(R.id.text_scan_progress);
        textConnectionResult = view.findViewById(R.id.text_connection_result);
        recyclerDiscoveredServers = view.findViewById(R.id.recycler_discovered_servers);
        
        // Get ServerConfigManager instance
        configManager = ServerConfigManager.getInstance();
        
        // Initialize ConnectionTester and LanServerScanner
        connectionTester = new ConnectionTester();
        lanServerScanner = new LanServerScanner();
        
        // Set up discovered servers RecyclerView
        setupDiscoveredServersRecyclerView();
        
        // Load current configuration
        loadCurrentConfiguration();
        
        // Set up real-time validation
        setupValidation();
        
        // Set up scan LAN button
        setupScanLanButton();
        
        // Set up test connection button
        setupTestConnectionButton();
        
        // Set up save button
        setupSaveButton();
        
        return view;
    }
    
    /**
     * Sets up the discovered servers RecyclerView.
     */
    private void setupDiscoveredServersRecyclerView() {
        recyclerDiscoveredServers.setLayoutManager(new LinearLayoutManager(getContext()));
        serversAdapter = new DiscoveredServersAdapter(server -> {
            // When user taps a server, auto-fill the configuration
            editServerIp.setText(server.getIpAddress());
            editServerPort.setText(String.valueOf(SCAN_PORT));
            showConnectionResult(true, "Server selected: " + server.getIpAddress());
        });
        recyclerDiscoveredServers.setAdapter(serversAdapter);
    }
    
    /**
     * Sets up the scan LAN button click handler.
     */
    private void setupScanLanButton() {
        btnScanLan.setOnClickListener(v -> scanLanServers());
    }
    
    /**
     * Scans the LAN for available game servers.
     */
    private void scanLanServers() {
        // Show scanning state
        setScanningState(true);
        
        // Clear previous results
        serversAdapter.clearServers();
        recyclerDiscoveredServers.setVisibility(View.GONE);
        
        // Start scan
        lanServerScanner.scanLan(new LanServerScanner.ScanCallback() {
            @Override
            public void onProgress(int scannedCount, int totalCount) {
                // Update progress on main thread
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        textScanProgress.setText("Scanning: " + scannedCount + "/" + totalCount);
                    });
                }
            }
            
            @Override
            public void onComplete(List<LanServerScanner.DiscoveredServer> servers) {
                // Update UI on main thread
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        setScanningState(false);
                        
                        if (servers.isEmpty()) {
                            showConnectionResult(false, "No servers found on LAN");
                        } else {
                            showConnectionResult(true, "Found " + servers.size() + " server(s)");
                            serversAdapter.setServers(servers);
                            recyclerDiscoveredServers.setVisibility(View.VISIBLE);
                        }
                    });
                }
            }
        });
    }
    
    /**
     * Sets the scanning state UI (loading indicator and button state).
     * 
     * @param isScanning true if scanning is in progress, false otherwise
     */
    private void setScanningState(boolean isScanning) {
        if (isScanning) {
            // Show loading indicator
            progressScanLan.setVisibility(View.VISIBLE);
            textScanProgress.setVisibility(View.VISIBLE);
            textScanProgress.setText("Scanning: 0/254");
            // Disable scan button
            btnScanLan.setEnabled(false);
            btnScanLan.setText("");
            // Hide previous result
            textConnectionResult.setVisibility(View.GONE);
        } else {
            // Hide loading indicator
            progressScanLan.setVisibility(View.GONE);
            textScanProgress.setVisibility(View.GONE);
            // Enable scan button
            btnScanLan.setEnabled(true);
            btnScanLan.setText("Scan LAN Servers");
        }
    }
    
    /**
     * Loads the current server configuration into the input fields.
     */
    private void loadCurrentConfiguration() {
        editServerIp.setText(configManager.getServerIp());
        editServerPort.setText(configManager.getServerPort());
    }
    
    /**
     * Sets up real-time validation for IP address and port inputs.
     */
    private void setupValidation() {
        // IP address validation
        editServerIp.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
            
            @Override
            public void afterTextChanged(Editable s) {
                validateIpAddress(s.toString());
            }
        });
        
        // Port validation
        editServerPort.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
            
            @Override
            public void afterTextChanged(Editable s) {
                validatePort(s.toString());
            }
        });
    }
    
    /**
     * Validates the IP address format.
     * Accepts IPv4 addresses in the format xxx.xxx.xxx.xxx where each octet is 0-255.
     * 
     * @param ip the IP address string to validate
     * @return true if valid, false otherwise
     */
    private boolean validateIpAddress(String ip) {
        if (ip == null || ip.trim().isEmpty()) {
            editServerIp.setError(null);
            return false;
        }
        
        String trimmedIp = ip.trim();
        
        // Check basic format: should have 3 dots
        String[] parts = trimmedIp.split("\\.");
        if (parts.length != 4) {
            editServerIp.setError("Invalid IP address format");
            return false;
        }
        
        // Validate each octet
        try {
            for (String part : parts) {
                if (part.isEmpty()) {
                    editServerIp.setError("Invalid IP address format");
                    return false;
                }
                int value = Integer.parseInt(part);
                if (value < 0 || value > 255) {
                    editServerIp.setError("IP address octets must be between 0 and 255");
                    return false;
                }
            }
        } catch (NumberFormatException e) {
            editServerIp.setError("Invalid IP address format");
            return false;
        }
        
        // Valid IP address
        editServerIp.setError(null);
        return true;
    }
    
    /**
     * Validates the port number.
     * Port must be in the range 1-65535.
     * 
     * @param port the port string to validate
     * @return true if valid, false otherwise
     */
    private boolean validatePort(String port) {
        if (port == null || port.trim().isEmpty()) {
            editServerPort.setError(null);
            return false;
        }
        
        try {
            int portNumber = Integer.parseInt(port.trim());
            if (portNumber < 1 || portNumber > 65535) {
                editServerPort.setError("Port must be between 1 and 65535");
                return false;
            }
            // Valid port
            editServerPort.setError(null);
            return true;
        } catch (NumberFormatException e) {
            editServerPort.setError("Invalid port number");
            return false;
        }
    }
    
    /**
     * Sets up the test connection button click handler.
     */
    private void setupTestConnectionButton() {
        btnTestConnection.setOnClickListener(v -> testConnection());
    }
    
    /**
     * Tests the connection to the configured server.
     * Shows loading indicator during test and displays result.
     */
    private void testConnection() {
        // Get current input values
        String serverIp = editServerIp.getText().toString().trim();
        String serverPort = editServerPort.getText().toString().trim();
        
        // Validate inputs before testing
        if (!validateIpAddress(serverIp)) {
            showConnectionResult(false, "Please enter a valid IP address");
            return;
        }
        
        if (!validatePort(serverPort)) {
            showConnectionResult(false, "Please enter a valid port number (1-65535)");
            return;
        }
        
        // Show loading state
        setTestingState(true);
        
        // Test connection asynchronously
        connectionTester.testConnection(serverIp, serverPort, result -> {
            // Update UI on main thread
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    setTestingState(false);
                    showConnectionResult(result.isSuccess(), result.getMessage());
                });
            }
        });
    }
    
    /**
     * Sets the testing state UI (loading indicator and button state).
     * 
     * @param isTesting true if testing is in progress, false otherwise
     */
    private void setTestingState(boolean isTesting) {
        if (isTesting) {
            // Show loading indicator
            progressTestConnection.setVisibility(View.VISIBLE);
            // Disable test button
            btnTestConnection.setEnabled(false);
            btnTestConnection.setText("");
            // Hide previous result
            textConnectionResult.setVisibility(View.GONE);
        } else {
            // Hide loading indicator
            progressTestConnection.setVisibility(View.GONE);
            // Enable test button
            btnTestConnection.setEnabled(true);
            btnTestConnection.setText("Test Connection");
        }
    }
    
    /**
     * Displays the connection test result.
     * 
     * @param success true if connection was successful, false otherwise
     * @param message the result message to display
     */
    private void showConnectionResult(boolean success, String message) {
        textConnectionResult.setVisibility(View.VISIBLE);
        textConnectionResult.setText(message);
        
        if (success) {
            // Green indicator for success
            textConnectionResult.setTextColor(Color.parseColor("#4CAF50"));
            textConnectionResult.setBackgroundColor(Color.parseColor("#E8F5E9"));
        } else {
            // Red indicator for failure
            textConnectionResult.setTextColor(Color.parseColor("#F44336"));
            textConnectionResult.setBackgroundColor(Color.parseColor("#FFEBEE"));
        }
    }
    
    /**
     * Sets up the save button click handler.
     */
    private void setupSaveButton() {
        btnSave.setOnClickListener(v -> saveConfiguration());
    }
    
    /**
     * Saves the server configuration.
     * Validates inputs, updates ServerConfigManager, and shows confirmation.
     */
    private void saveConfiguration() {
        // Get current input values
        String serverIp = editServerIp.getText().toString().trim();
        String serverPort = editServerPort.getText().toString().trim();
        
        // Validate inputs before saving
        if (!validateIpAddress(serverIp)) {
            showConnectionResult(false, "Please enter a valid IP address");
            return;
        }
        
        if (!validatePort(serverPort)) {
            showConnectionResult(false, "Please enter a valid port number (1-65535)");
            return;
        }
        
        // Update ServerConfigManager with new values
        // This will trigger storage persistence and WebSocket disconnection via the listener mechanism
        configManager.setServerAddress(serverIp, serverPort);
        
        // Show confirmation message
        showConnectionResult(true, "Configuration saved");
    }
    
    /**
     * RecyclerView adapter for displaying discovered servers.
     */
    private static class DiscoveredServersAdapter extends RecyclerView.Adapter<DiscoveredServersAdapter.ServerViewHolder> {
        
        private List<LanServerScanner.DiscoveredServer> servers = new java.util.ArrayList<>();
        private final OnServerClickListener listener;
        
        interface OnServerClickListener {
            void onServerClick(LanServerScanner.DiscoveredServer server);
        }
        
        DiscoveredServersAdapter(OnServerClickListener listener) {
            this.listener = listener;
        }
        
        void setServers(List<LanServerScanner.DiscoveredServer> servers) {
            this.servers = new java.util.ArrayList<>(servers);
            notifyDataSetChanged();
        }
        
        void clearServers() {
            this.servers.clear();
            notifyDataSetChanged();
        }
        
        @NonNull
        @Override
        public ServerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_discovered_server, parent, false);
            return new ServerViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull ServerViewHolder holder, int position) {
            LanServerScanner.DiscoveredServer server = servers.get(position);
            holder.bind(server, listener);
        }
        
        @Override
        public int getItemCount() {
            return servers.size();
        }
        
        static class ServerViewHolder extends RecyclerView.ViewHolder {
            private final TextView textServerIp;
            private final TextView textServerResponseTime;
            
            ServerViewHolder(@NonNull View itemView) {
                super(itemView);
                textServerIp = itemView.findViewById(R.id.text_server_ip);
                textServerResponseTime = itemView.findViewById(R.id.text_server_response_time);
            }
            
            void bind(LanServerScanner.DiscoveredServer server, OnServerClickListener listener) {
                textServerIp.setText(server.getIpAddress());
                textServerResponseTime.setText("Response time: " + server.getResponseTimeMs() + "ms");
                
                itemView.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onServerClick(server);
                    }
                });
            }
        }
    }
}
