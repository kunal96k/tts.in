# AWS EC2 (Linux) Production Deployment Guide
This guide outlines the process of building, deploying, and configuring the **Enquiries Management System** on an **AWS EC2 Amazon Linux** instance behind an **Nginx** reverse proxy, running as a background service.

---

## 📋 Prerequisites & Details
* **AWS Linux Instance Host:** `ec2-13-203-216-126.ap-south-1.compute.amazonaws.com` (IP: `13.203.216.126`)
* **SSH Key File:** `all_instance.pem`
* **Local OS:** Windows
* **Target Port:** `8080` (Application Port) mapped to public HTTP `80` (via Nginx)

---

## 🛠️ Step 1: Package the Application (WAR file)
Ensure you have the latest compiled files and package the project as a `.war` file:
1. Open terminal in `c:\Users\TechnoKraft\Desktop\TTS\tts\enquiries`
2. Run the packaging command:
   ```bash
   mvn clean package -DskipTests
   ```
3. This creates a deployable executable WAR file at:
   `target/enquiries-0.0.1-SNAPSHOT.war`

---

## 📤 Step 2: Upload the WAR File to AWS EC2
Use Secure Copy Protocol (`scp`) to copy the generated `.war` file from your local machine to the EC2 home folder:
1. Open your terminal in the directory where your `all_instance.pem` key is stored.
2. Run the upload command:
   ```bash
   scp -i "all_instance.pem" "c:/Users/TechnoKraft/Desktop/TTS/tts/enquiries/target/enquiries-0.0.1-SNAPSHOT.war" ec2-user@ec2-13-203-216-126.ap-south-1.compute.amazonaws.com:/home/ec2-user/
   ```

---

## 🖥️ Step 3: Install Java 21 on AWS EC2
Connect to your EC2 instance and install the Amazon Corretto JDK 21 distribution:
1. SSH into the server:
   ```bash
   ssh -i "all_instance.pem" ec2-user@ec2-13-203-216-126.ap-south-1.compute.amazonaws.com
   ```
2. Install Java 21:
   ```bash
   sudo dnf install java-21-amazon-corretto-devel -y
   ```
3. Verify the installation:
   ```bash
   java -version
   ```

---

## 🗄️ Step 4: Configure the Database on AWS
Our application relies on MySQL. You have two options:
* **Option A: Setup local MySQL on the EC2 instance**
* **Option B: Connect to an AWS RDS MySQL Instance (Recommended for Prod)**

For a local setup on Amazon Linux 2023:
1. Install MySQL repository and server:
   ```bash
   sudo dnf install mariadb105-server -y
   ```
2. Start and enable database service:
   ```bash
   sudo systemctl start mariadb
   sudo systemctl enable mariadb
   ```
3. Secure your installation and set root password:
   ```bash
   sudo mysql_secure_installation
   ```
4. Create database:
   ```bash
   mysql -u root -p -e "CREATE DATABASE enquiries_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
   ```

---

## ⚙️ Step 5: Configure the Systemd Service
To ensure the application runs continuously in the background and restarts automatically if the server reboots:
1. Create a service file:
   ```bash
   sudo nano /etc/systemd/system/enquiries.service
   ```
2. Paste the following configuration (modifying database credentials accordingly):
   ```ini
   [Unit]
   Description=Enquiries Management System (Spring Boot)
   After=syslog.target network.target mariadb.service

   [Service]
   User=ec2-user
   WorkingDirectory=/home/ec2-user
   ExecStart=/usr/bin/java -jar enquiries-0.0.1-SNAPSHOT.war --spring.profiles.active=prod
   SuccessExitStatus=143
   Restart=always
   RestartSec=10

   # Env Overrides for Production Security
   Environment="SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/enquiries_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Kolkata"
   Environment="SPRING_DATASOURCE_USERNAME=root"
   Environment="SPRING_DATASOURCE_PASSWORD=your_mysql_password_here"
   Environment="APP_ADMIN_USERNAME=admin@technokraft.com"
   Environment="APP_ADMIN_PASSWORD=Admin@123"
   Environment="APP_ADMIN_NAME=TechnoKraft Admin"

   [Install]
   WantedBy=multi-user.target
   ```
3. Reload systemd, enable, and start the service:
   ```bash
   sudo systemctl daemon-reload
   sudo systemctl enable enquiries.service
   sudo systemctl start enquiries.service
   ```
4. Check the service status and logs:
   ```bash
   sudo systemctl status enquiries.service
   sudo journalctl -u enquiries.service -f
   ```

---

## 🔀 Step 6: Configure Nginx as a Reverse Proxy & Custom Error Pages
Nginx accepts incoming HTTP requests on port 80 and forwards them to our Spring Boot app running on port 8080. It is also configured to intercept server-level/gateway errors (like 502 Bad Gateway during restarts) and show clean static error pages while passing standard application errors (like 404 and 500) directly to Spring Boot.

### Step 6.1: Create Nginx Routing Configuration
1. Install Nginx:
   ```bash
   sudo dnf install nginx -y
   ```
2. Create Nginx routing configuration file:
   ```bash
   sudo nano /etc/nginx/conf.d/enquiries.conf
   ```
3. Paste the following configuration:
   ```nginx
   server {
       listen 80;
       server_name 13.203.216.126 ec2-13-203-216-126.ap-south-1.compute.amazonaws.com;

       location / {
           proxy_pass http://127.0.0.1:8080;
           proxy_set_header Host $host;
           proxy_set_header X-Real-IP $remote_addr;
           proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
           proxy_set_header X-Forwarded-Proto $scheme;
           
           # Increase maximum upload file limit for exports / data transfers
           client_max_body_size 15M;
       }

       # Custom gateway error pages when the Spring Boot application is offline/restarting
       error_page 502 /error_pages/502.html;
       error_page 503 /error_pages/503.html;
       error_page 504 /error_pages/504.html;

       location /error_pages/ {
           root /var/www/html;
           internal;
       }
   }
   ```
4. Start and enable Nginx:
   ```bash
   sudo systemctl restart nginx
   sudo systemctl enable nginx
   ```

### Step 6.2: Deploy Custom Static Error Pages
When the Spring Boot application is restarting or offline, it cannot serve its internal resources. We must deploy the static error files directly to the Nginx root directory:
1. Create the static directory on the server:
   ```bash
   sudo mkdir -p /var/www/html/error_pages
   ```
2. Copy the pre-built error page files (`502.html`, `503.html`, `504.html`) to the server. You can do this by copying them from the unpacked resources folder or by extracting them from the uploaded WAR file:
   * **If copying from local machine to server**:
     ```bash
     scp -i "all_instance.pem" -r "c:/Users/TechnoKraft/Desktop/TTS/tts/enquiries/src/main/resources/static/error_pages/"* ec2-user@ec2-13-203-216-126.ap-south-1.compute.amazonaws.com:/home/ec2-user/
     ```
     Then move them on the server:
     ```bash
     sudo mv /home/ec2-user/502.html /home/ec2-user/503.html /home/ec2-user/504.html /var/www/html/error_pages/
     ```
3. Set the correct permissions so Nginx can read the custom error pages:
   ```bash
   sudo chmod -R 755 /var/www/html/error_pages
   sudo chown -R nginx:nginx /var/www/html/error_pages
   ```

---

## 🔒 Step 7: Open Port 80 in AWS Security Group
To make the dashboard accessible via the server IP in your browser:
1. Go to your **AWS Console** -> **EC2 Dashboard** -> **Instances**.
2. Click on your instance (`ec2-13-203-216-126`).
3. Click the **Security** tab at the bottom, then click on the active **Security Group**.
4. Click **Edit inbound rules**.
5. Add a new rule:
   * **Type:** `HTTP`
   * **Port:** `80`
   * **Source:** `Anywhere-IPv4` (`0.0.0.0/0`)
6. Save the rules.

Now, you can access the application securely in your browser directly using the IP: `http://13.203.216.126/dashboard`!
