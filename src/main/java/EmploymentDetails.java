public class EmploymentDetails {

    private String employerName;
    private String jobTitle;
    private double annualSalary;

    public EmploymentDetails(String employerName,
                            String jobTitle,
                            double annualSalary) {
        this.employerName = employerName;
        this.jobTitle = jobTitle;
        this.annualSalary = annualSalary;
    }

    public String getEmployerName() {
        return employerName;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public double getAnnualSalary() {
        return annualSalary;
    }

    public void setEmployerName(String employerName) {
        this.employerName = employerName;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public void setAnnualSalary(double annualSalary) {
        this.annualSalary = annualSalary;
    }
}
