# 🛒 ShopHub – Full Stack E-Commerce Platform

<div align="center">

# ShopHub

### Modern E-Commerce Platform with Secure Authentication, Subscription Plans, Stripe Payments & Admin Dashboard

[![React](https://img.shields.io/badge/React-Frontend-61DAFB?logo=react)]()
[![Spring Boot](https://img.shields.io/badge/SpringBoot-Backend-6DB33F?logo=springboot)]()
[![MySQL](https://img.shields.io/badge/MySQL-Database-4479A1?logo=mysql)]()
[![JWT](https://img.shields.io/badge/JWT-Authentication-black)]()
[![Stripe](https://img.shields.io/badge/Stripe-Payments-635BFF?logo=stripe)]()
[![License](https://img.shields.io/badge/License-MIT-green)]()

</div>

---

## 🌐 Live Demo

### 🚀 Application URL

**🔗 Live Website:**  
https://mini-project-frontend-iota-eight.vercel.app/

> ⚠️ **Important Note**
>
> The backend is deployed on **Render Free Tier**.
>
> If the application has been inactive for some time, the backend server may enter sleep mode.
>
> The first request can take **30–60 seconds** (sometimes longer) while Render wakes up the server.
>
> After activation, the application performs normally.

---

# 📖 Overview

ShopHub is a modern full-stack e-commerce web application designed to provide a complete online shopping experience.

The platform enables users to:

- Browse products
- Search and filter products
- Manage shopping carts
- Make secure payments
- Subscribe to premium plans
- Manage personal profiles
- Upload profile images
- View locations on maps

The system also includes a powerful admin dashboard for managing users, products, subscriptions, and platform operations.

---

# ✨ Key Features

## 👤 User Features

### Authentication

- User Registration
- Secure Login
- Logout
- Email Verification
- JWT Authentication
- Google OAuth Login
- GitHub OAuth Login
- Protected Routes

### Shopping Features

- Product Listing
- Product Details Page
- Category Filtering
- Product Search
- Add To Cart
- Remove From Cart
- Quantity Management
- Checkout Process

### Subscription System

- Free Plan
- Silver Plan
- Gold Plan
- Plan Upgrades
- Subscription Tracking
- Automatic Expiration

### Profile Management

- Profile Update
- Profile Image Upload
- Address Management
- Location Suggestions
- Google Maps Integration
- Profile Export

### Payments

- Stripe Payment Gateway
- Secure Transactions
- Subscription Payments
- Payment Verification

---

## 🛠 Admin Features

### Dashboard

- Total Users Statistics
- Premium User Monitoring
- Admin Monitoring

### User Management

- View Users
- Search Users
- Filter Users
- Pagination
- Manage Roles
- Delete Users

### Product Management

- Add Products
- Update Products
- Delete Products
- Manage Categories

### Subscription Management

- Monitor Plans
- View Active Plans
- View Expired Plans
- Manage Access

---

# 🚀 Technologies Used

## Frontend

| Technology | Purpose |
|------------|----------|
| React.js | UI Development |
| JavaScript (ES6+) | Frontend Logic |
| Tailwind CSS | Styling |
| React Router | Routing |
| Axios | API Calls |
| React Hot Toast | Notifications |
| shadcn/ui | UI Components |

---

## Backend

| Technology | Purpose |
|------------|----------|
| Spring Boot | Backend Framework |
| Spring Security | Security |
| JWT | Authentication |
| Hibernate / JPA | ORM |
| Lombok | Boilerplate Reduction |
| Maven | Dependency Management |

---

## Database

| Technology | Purpose |
|------------|----------|
| MySQL | Data Storage |

---

## Third Party Services

### Authentication

- Google OAuth
- GitHub OAuth

### Email Service

- SendGrid

### Storage

- Supabase Cloud Storage

### Payments

- Stripe

### Maps

- LocationIQ
- Google Maps

---

# 🏗 Architecture

```text
┌─────────────────────────┐
│       React Frontend    │
└────────────┬────────────┘
             │
             ▼
┌─────────────────────────┐
│    Spring Boot APIs     │
└────────────┬────────────┘
             │
             ▼
┌─────────────────────────┐
│        MySQL DB         │
└─────────────────────────┘

External Services:
──────────────────────
• Stripe
• Google OAuth
• GitHub OAuth
• SendGrid
• Supabase
• Google Maps
• LocationIQ
```

---

# 🔐 Authentication Flow

```text
User Signup
      │
      ▼
Email Verification
      │
      ▼
Login
      │
      ▼
JWT Token Generation
      │
      ▼
Protected Routes Access
```

---

# 💳 Subscription Plans

## Free Plan

- Basic Access
- Permanent Access

---

## Silver Plan

Price: $5

Features:

- Discounted Products
- Extended Access
- Faster Session

---

## Gold Plan

Price: $10

Features:

- Premium Access
- Full Features
- Global Product Access
- Priority Experience

---

# 📂 Project Structure

```bash
ShopHub/
│
├── frontend/
│   │
│   ├── src/
│   ├── components/
│   ├── pages/
│   ├── hooks/
│   ├── context/
│   ├── services/
│   └── assets/
│
├── backend/
│   │
│   ├── controllers/
│   ├── services/
│   ├── repositories/
│   ├── entities/
│   ├── security/
│   ├── config/
│   └── dto/
│
├── database/
│
├── docs/
│
└── README.md
```

---

# ⚙ Installation Guide

## Clone Repository

```bash
git clone https://github.com/your-username/shophub.git

cd shophub
```

---

# Frontend Setup

```bash
cd frontend

npm install

npm run dev
```

Application starts at:

```bash
http://localhost:5173
```

---

# Backend Setup

```bash
cd backend

mvn clean install

mvn spring-boot:run
```

Application starts at:

```bash
http://localhost:8080
```

---

# Database Setup

Create database:

```sql
CREATE DATABASE shophub;
```

Configure:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/shophub

spring.datasource.username=root

spring.datasource.password=password
```

---

# Environment Variables

## Frontend

```env
VITE_API_URL=
VITE_GOOGLE_MAP_API=
```

---

## Backend

```env
DATABASE_URL=
DATABASE_USERNAME=
DATABASE_PASSWORD=

JWT_SECRET=

STRIPE_SECRET_KEY=

SENDGRID_API_KEY=

GOOGLE_CLIENT_ID=
GOOGLE_CLIENT_SECRET=

GITHUB_CLIENT_ID=
GITHUB_CLIENT_SECRET=

SUPABASE_URL=
SUPABASE_KEY=
```

---

# API Modules

## Authentication APIs

```http
POST /api/auth/register

POST /api/auth/login

POST /api/auth/verify-email

POST /api/auth/logout
```

---

## Product APIs

```http
GET /api/products

GET /api/products/{id}

POST /api/products

PUT /api/products/{id}

DELETE /api/products/{id}
```

---

## Cart APIs

```http
POST /api/cart/add

GET /api/cart

DELETE /api/cart/remove
```

---

## Payment APIs

```http
POST /api/payment/create-session

POST /api/payment/success
```

---

## Admin APIs

```http
GET /api/admin/users

GET /api/admin/products

GET /api/admin/dashboard
```

---

# 📸 Screenshots

## Authentication

- Signup Page
- Login Page
- Email Verification

## Shopping

- Product Listing
- Product Details
- Cart Page
- Checkout Page

## Subscription

- Pricing Plans
- Stripe Checkout

## User Profile

- Profile Dashboard
- Image Upload
- Location Mapping

## Admin

- Admin Dashboard
- User Management
- Product Management

---

# 🧪 Testing

## Testing Performed

### Unit Testing

- User Registration
- Login
- Product APIs
- Cart APIs

### Integration Testing

- Frontend ↔ Backend
- Backend ↔ Database
- Stripe ↔ Backend
- Maps ↔ Frontend

### System Testing

- Complete Shopping Flow
- Subscription Flow
- Admin Operations

---

# 🔄 Cron Job Automation

Automated background jobs:

### Plan Expiration

- Detect Expired Plans
- Revoke Premium Access
- Update User Status

### Subscription Monitoring

- Auto Status Updates
- Expiration Tracking

---

# 🔒 Security Features

- JWT Authentication
- Password Encryption
- BCrypt Password Hashing
- Email Verification
- Protected Routes
- Role-Based Access Control
- Secure API Access
- Authentication Middleware

---

# 📈 Future Enhancements

### Planned Features

- AI Product Recommendations
- Wishlist Feature
- Live Order Tracking
- Mobile Application
- Vendor Management
- Analytics Dashboard
- Coupons & Discounts
- Refund System
- Multi-language Support
- Push Notifications
- Chat Support

---

# 🤝 Contributing

Contributions are welcome.

### Steps

```bash
1. Fork Repository

2. Create New Branch

git checkout -b feature-name

3. Commit Changes

git commit -m "Added Feature"

4. Push Changes

git push origin feature-name

5. Create Pull Request
```

---

# ⭐ Support

If you like this project:

⭐ Star the repository

🍴 Fork the project

🛠 Contribute improvements

---

# 👨‍💻 Author

## Aakash Singh

Master of Computer Application (MCA)

Jagan Institute of Management Studies

Guru Gobind Singh Indraprastha University

Batch: 2024–2026

---

<div align="center">

### 🚀 ShopHub – Bringing Modern E-Commerce to Life

Built with ❤️ using React, Spring Boot, MySQL, Stripe & Modern Web Technologies

</div>
