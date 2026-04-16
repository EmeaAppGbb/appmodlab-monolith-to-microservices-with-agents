package com.eduverse.events;

public class CertificateIssuedEvent extends DomainEvent {

    private Long certificateId;
    private Long enrollmentId;
    private Long studentId;
    private String certificateNumber;
    private String pdfUrl;

    public CertificateIssuedEvent() {
    }

    public CertificateIssuedEvent(Long certificateId, Long enrollmentId, Long studentId, String certificateNumber, String pdfUrl) {
        this.certificateId = certificateId;
        this.enrollmentId = enrollmentId;
        this.studentId = studentId;
        this.certificateNumber = certificateNumber;
        this.pdfUrl = pdfUrl;
    }

    @Override
    public String getEventType() {
        return "CertificateIssued";
    }

    public Long getCertificateId() {
        return certificateId;
    }

    public void setCertificateId(Long certificateId) {
        this.certificateId = certificateId;
    }

    public Long getEnrollmentId() {
        return enrollmentId;
    }

    public void setEnrollmentId(Long enrollmentId) {
        this.enrollmentId = enrollmentId;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public String getCertificateNumber() {
        return certificateNumber;
    }

    public void setCertificateNumber(String certificateNumber) {
        this.certificateNumber = certificateNumber;
    }

    public String getPdfUrl() {
        return pdfUrl;
    }

    public void setPdfUrl(String pdfUrl) {
        this.pdfUrl = pdfUrl;
    }

    @Override
    public String toString() {
        return "CertificateIssuedEvent{" +
                "certificateId=" + certificateId +
                ", enrollmentId=" + enrollmentId +
                ", studentId=" + studentId +
                ", certificateNumber='" + certificateNumber + '\'' +
                ", pdfUrl='" + pdfUrl + '\'' +
                ", eventId='" + getEventId() + '\'' +
                ", occurredAt=" + getOccurredAt() +
                '}';
    }
}
