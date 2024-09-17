package com.windstrom5.tugasakhir.connection

import com.windstrom5.tugasakhir.model.Perusahaan
import com.windstrom5.tugasakhir.model.response
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.DELETE
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @Multipart
    @POST("DaftarPerusahaan")
    fun uploadPerusahaan(
        @Part("nama") nama: RequestBody,
        @Part("latitude") latitude: RequestBody,
        @Part("longitude") longitude: RequestBody,
        @Part("jam_masuk") jamMasuk: RequestBody,
        @Part("jam_keluar") jamKeluar: RequestBody,
        @Part("batas_aktif") batasAktif: RequestBody,
        @Part("secret_key") secretKey: RequestBody,
        @Part logo: MultipartBody.Part?
    ): Call<ApiResponse>

    @Multipart
    @POST("Perusahaan/DaftarAdmin")
    fun uploadAdmin(
        @Part("nama_perusahaan") nama_perusahaan: RequestBody,
        @Part("email") email: RequestBody,
        @Part("password") password: RequestBody,
        @Part("nama") nama: RequestBody,
        @Part("tanggal_lahir") tanggal_lahir: RequestBody,
        @Part profilePart: MultipartBody.Part?
    ): Call<ApiResponse>

    @Multipart
    @POST("Perusahaan/DaftarPekerja")
    fun uploadPekerja(
        @Part("nama_perusahaan") nama_perusahaan: RequestBody,
        @Part("email") email: RequestBody,
        @Part("password") password: RequestBody,
        @Part("nama") nama: RequestBody,
        @Part("tanggal_lahir") tanggal_lahir: RequestBody,
        @Part profile: MultipartBody.Part?
    ): Call<ApiResponse>

    @Multipart
    @POST("Lembur/AddLembur")
    fun uploadLembur(
        @Part("nama_perusahaan") nama_perusahaan: RequestBody,
        @Part("nama") nama: RequestBody,
        @Part("tanggal") tanggal: RequestBody,
        @Part("waktu_masuk") waktu_masuk: RequestBody,
        @Part("waktu_pulang") waktu_keluar: RequestBody,
        @Part("pekerjaan") pekerjaan: RequestBody,
        @Part bukti: MultipartBody.Part
    ): Call<ApiResponse>

    @Multipart
    @POST("Dinas/AddDinas")
    fun uploadDinas(
        @Part("nama_perusahaan") nama_perusahaan: RequestBody,
        @Part("nama") nama: RequestBody,
        @Part("tujuan") tujuan: RequestBody,
        @Part("tanggal_berangkat") tanggal_berangkat: RequestBody,
        @Part("tanggal_pulang") tanggal_pulang: RequestBody,
        @Part("kegiatan") kegiatan: RequestBody,
        @Part bukti: MultipartBody.Part
    ): Call<ApiResponse>

    @Multipart
    @POST("Izin/AddIzin")
    fun uploadIzin(
        @Part("nama_perusahaan") nama_perusahaan: RequestBody,
        @Part("nama") nama: RequestBody,
        @Part("tanggal") tanggal: RequestBody,
        @Part("kategori") kategori: RequestBody,
        @Part("alasan") alasan: RequestBody,
        @Part bukti: MultipartBody.Part
    ): Call<ApiResponse>

    @GET("Presensi/getLocation/{nama_perusahaan}")
    fun getLocationPekerja(
        @Path("nama_perusahaan") nama_perusahaan: String
    ): Call<ResponseBody>

    @GET("Perusahaan/getAnggota/{nama_perusahaan}")
    fun getDataPekerja(
        @Path("nama_perusahaan") nama_perusahaan: String
    ): Call<ResponseBody>

    @GET("Lembur/getDataLemburPerusahaan/{nama_perusahaan}")
    fun getDataLemburPerusahaan(
        @Path("nama_perusahaan") nama_perusahaan: String
    ): Call<ResponseBody>
    @GET("Lembur/getDataLemburPekerja/{nama_perusahaan}/{nama_pekerja}")
    fun getDataLemburPekerja(
        @Path("nama_perusahaan") nama_perusahaan: String,
        @Path("nama_pekerja") nama_pekerja: String
    ): Call<ResponseBody>
    @GET("Dinas/getDataDinasPerusahaan/{nama_perusahaan}")
    fun getDataDinasPerusahaan(
        @Path("nama_perusahaan") nama_perusahaan: String
    ): Call<ResponseBody>
    @GET("Presensi/getDataAbsenPekerja/{nama_perusahaan}/{nama_pekerja}")
    fun getDataAbsenPekerja(
        @Path("nama_perusahaan") nama_perusahaan: String,
        @Path("nama_pekerja") nama_pekerja: String
    ): Call<ResponseBody>
    @GET("Presensi/getDataAbsenPerusahaan/{nama_perusahaan}")
    fun getDataAbsenPerusahaan(
        @Path("nama_perusahaan") nama_perusahaan: String
    ): Call<ResponseBody>
    @GET("Dinas/getDataDinasPekerja/{nama_perusahaan}/{nama_pekerja}")
    fun getDataDinasPekerja(
        @Path("nama_perusahaan") nama_perusahaan: String,
        @Path("nama_pekerja") nama_pekerja: String
    ): Call<ResponseBody>

    @GET("Izin/getDataIzinPerusahaan/{nama_perusahaan}")
    fun getDataIzinPerusahaan(
        @Path("nama_perusahaan") nama_perusahaan: String
    ): Call<ResponseBody>

    @GET("Izin/getDataIzinPekerja/{nama_perusahaan}/{nama_pekerja}")
    fun getDataIzinPekerja(
        @Path("nama_perusahaan") nama_perusahaan: String,
        @Path("nama_pekerja") nama_pekerja: String
    ): Call<ResponseBody>

    @POST("Izin/UpdateStatusIzin?_method=PUT")
    fun updatestatusIzin(
        @Query("id") id: Int,
        @Query("status") status: String
    ): Call<ApiResponse>

    @POST("Dinas/UpdateStatusDinas?_method=PUT")
    fun updatestatusDinas(
        @Query("id") id: Int,
        @Query("status") status: String
    ): Call<ApiResponse>

    @PUT("Lembur/UpdateStatusLembur")
    fun updatestatusLembur(
        @Query("id") id: Int,
        @Query("status") status: String
    ): Call<ApiResponse>
    @Multipart
    @POST("Izin/UpdateDataIzin/{izinId}?_method=PUT")
    fun updateIzin(
        @Path("izinId") izinId: Int,
        @Part("tanggal") tanggal: RequestBody,
        @Part("kategori") kategori: RequestBody,
        @Part("alasan") alasan: RequestBody,
        @Part bukti: MultipartBody.Part?
    ): Call<ApiResponse>
    @Multipart
    @POST("Lembur/UpdateDataLembur/{lemburId}?_method=PUT")
    fun updateLembur(
        @Path("lemburId") lemburId: Int,
        @Part("tanggal") tanggal: RequestBody,
        @Part("masuk") masuk: RequestBody,
        @Part("pulang") pulang: RequestBody,
        @Part("pekerjaan") pekerjaan: RequestBody,
        @Part bukti: MultipartBody.Part?
    ): Call<ApiResponse>
    @Multipart
    @POST("Dinas/UpdateDataDinas/{dinasId}?_method=PUT")
    fun updateDinas(
        @Path("dinasId") dinasId: Int,
        @Part("berangkat") berangkat: RequestBody,
        @Part("pulang") pulang: RequestBody,
        @Part("tujuan") tujuan: RequestBody,
        @Part("kegiatan") kegiatan: RequestBody,
        @Part bukti: MultipartBody.Part?
    ): Call<ApiResponse>
    @Multipart
    @POST("Perusahaan/UpdateDataUser/{id}?_method=PUT")
    fun updatePekerja(
        @Part("id") id: Int,
        @Part("email") email: RequestBody,
        @Part("nama") nama: RequestBody,
        @Part("tanggal_lahir") tanggal_lahir: RequestBody,
        @Part profile: MultipartBody.Part?
    ): Call<ApiResponse>

    @Multipart
    @POST("Perusahaan/UpdateDataUser/{id}?_method=PUT")
    fun updatePekerjaNoFile(
        @Part("id") id: Int,
        @Part("email") email: RequestBody,
        @Part("nama") nama: RequestBody,
        @Part("tanggal_lahir") tanggal_lahir: RequestBody
    ): Call<ApiResponse>

    @Multipart
    @POST("Perusahaan/UpdateDataAdmin/{id}?_method=PUT")
    fun updateAdmin(
        @Path("id") id: Int,  // Ensure this is @Path, not @Part
        @Part("email") emailEdit: RequestBody,
        @Part("nama") namaEdit: RequestBody,
        @Part("tanggal_lahir") tanggal_lahirEdit: RequestBody,
        @Part profile: MultipartBody.Part?
    ): Call<ApiResponse>

    @DELETE("DeletePerusahaan/{id}")
    fun deleteCompany(@Path("id") id: Int): Call<Void>

    @GET("GetPerusahaan")
    fun getPerusahaan():Call<ResponseBody>
    @GET("getData")
    fun getData(
        @Query("id") id: Int,
        @Query("jenis") jenis: String
    ): Call<ApiResponse>
    @GET("Perusahaan/{nama_perusahaan}")
    fun getPerusahaan(@Path("nama_perusahaan") namaPerusahaan: String): Call<Perusahaan>

    @GET("checkEmail/{email}")
    fun checkEmail(@Path("email") email: String): Call<Map<String, Any>>

    @Multipart
    @POST("resetPassword")
    fun resetPassword(
        @Path("email") email: String,
    ): Call<ApiResponse>

    @Multipart
    @POST("Perusahaan/UpdateDataPerusahaan/{id}?_method=PUT")
    fun updatePerusahaan(
        @Path("id") id: Int,
        @Part("nama") nama: RequestBody,
        @Part("jammasuk") jammasuk: RequestBody,
        @Part("jamkeluar") jamkeluar: RequestBody,
        @Part("latitude") latitude: RequestBody,
        @Part("longitude") longitude: RequestBody,
        @Part logo: MultipartBody.Part?
    ): Call<ApiResponse>

    @Multipart
    @POST("Lembur/Session/UpdateSession/{id}?_method=PUT")
    fun UpdateSessionLembur(
        @Path("id") id: Int?,
        @Part("jam") jam: RequestBody,
        @Part("keterangan") keterangan: RequestBody,
        @Part("bukti") bukti: MultipartBody.Part?,
    ): Call<ApiResponse>

    @Multipart
    @POST("Lembur/Session/AddSession")
    fun AddSessionLembur(
        @Part("id_lembur") id_lembur: RequestBody,
        @Part("jam") jam: RequestBody,
        @Part("keterangan") keterangan: RequestBody,
        @Part("bukti") bukti: MultipartBody.Part,
    ): Call<ApiResponse>
}

