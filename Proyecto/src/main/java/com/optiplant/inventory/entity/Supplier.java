 @Column(nullable = false, length = 150)
    private String name;

    @Column(unique = true, length = 50)
    private String nit;

    @Column(length = 150)
    private String email;

    @Column(length = 30)
    private String phone;

    @Column(length = 255)
    private String address;

    @Column(name = "contact_person", length = 120)
    private String contactPerson;

    @Column(nullable = false)
    private Boolean active = true;
}