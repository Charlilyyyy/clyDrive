# Problem Statement

## Project Name

**clyDrive** — Cloud File Storage System (Mini Google Drive)

## The Problem

Individuals and small teams need a reliable way to store, organize, and access digital files from anywhere. Local storage on a single device is fragile: files are lost when hardware fails, hard to share with others, and impossible to reach when away from that machine.

Existing commercial platforms (Google Drive, Dropbox, OneDrive) solve this at scale, but building a similar system is a strong way to learn production backend engineering — authentication, authorization, file I/O, metadata modeling, quotas, and secure sharing.

## Who Has This Problem?

| Persona | Pain |
|---------|------|
| **Regular user** | Wants one place for documents, images, and videos with folder organization |
| **Collaborator** | Needs to share a file via a link without exposing the whole account |
| **Administrator** | Must manage accounts, enforce limits, and monitor system usage |

## What We Are Building

A **backend REST API** that acts as a personal cloud drive:

- Users register, verify email, and log in securely
- Users upload, download, rename, move, and delete files
- Users organize files in nested folders
- Users share files through time-limited secure links
- Each user has a storage quota; the system tracks usage
- Admins manage users, roles, and system statistics

This is **not** a full Google Drive clone. It is a focused, learning-oriented backend that implements the core storage workflows with industry-standard security patterns.

## Why Build It?

1. **Learn real backend skills** — Spring Boot, JWT, JPA, file streaming, scheduled jobs
2. **Practice security** — RBAC, token rotation, account lockout, password policies
3. **Model a familiar domain** — folders, files, shares, and quotas map cleanly to database design
4. **Portfolio value** — demonstrates end-to-end system design from requirements to deployment
