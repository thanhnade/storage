package Model;

public class NguoiDung {
    private String taiKhoan;
    private String matKhau;
    private String thuMuc;
    private String role;
    private long storageLimit;
    
    public NguoiDung() {}
    
    public NguoiDung(String taiKhoan, String matKhau, String thuMuc, String role, long storageLimit) {
        this.taiKhoan = taiKhoan;
        this.matKhau = matKhau;
        this.thuMuc = thuMuc;
        this.role = role;
        this.storageLimit = storageLimit;
    }
    
    // Getters and Setters


    public String getTaiKhoan() {
        return taiKhoan;
    }
    
    public void setTaiKhoan(String taiKhoan) {
        this.taiKhoan = taiKhoan;
    }
    
    public String getMatKhau() {
        return matKhau;
    }
    
    public void setMatKhau(String matKhau) {
        this.matKhau = matKhau;
    }
    
    public String getThuMuc() {
        return thuMuc;
    }
    
    public void setThuMuc(String thuMuc) {
        this.thuMuc = thuMuc;
    }
    
    public String getRole() {
        return role;
    }
    
    public void setRole(String role) {
        this.role = role;
    }
    
    public long getStorageLimit() {
        return storageLimit;
    }
    
    public void setStorageLimit(long storageLimit) {
        this.storageLimit = storageLimit;
    }
} 